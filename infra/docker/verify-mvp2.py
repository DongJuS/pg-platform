"""MVP2 financial acceptance; only a newly created pg_mvp2_* project is permitted."""
import argparse
import base64
import urllib.parse
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime
from decimal import Decimal
import json
from pathlib import Path
import subprocess
import time
import urllib.error
import urllib.request
import uuid
from zoneinfo import ZoneInfo

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('run_directory', type=Path)
root = parser.parse_args().run_directory.resolve()
context = json.loads((root / 'context.json').read_text())
project = context['project']
if not project.startswith(('pg_mvp2_', 'pg_mvp3_', 'pg_mvp4_','pg_mvp5_')) or (root / 'mvp2-result.json').exists():
    raise SystemExit('Refusing financial acceptance outside a fresh isolated MVP2 run')
secrets = dict(line.split('=', 1) for line in (root/'local.env').read_text().splitlines() if '=' in line and not line.startswith('#'))
report = {'passed': False, 'checks': [], 'financial_mutations': 0, 'opening_funds_fixture_only': True}
def save():
    (root/'mvp2-result.json').write_text(json.dumps(report, indent=2))
def check(name, valid):
    report['checks'].append({'name': name, 'passed': bool(valid)})
    save()
    print(('PASS ' if valid else 'FAIL ') + name, flush=True)
    assert valid, name

def compose(*args):
    return subprocess.check_output(['docker','compose','-f',str(root/'compose.json'),*args],text=True).strip()
base = 'http://' + compose('port','dev-access','8080')
pgbase = 'http://' + compose('port','dev-access','8092')
paymentbase = 'http://' + compose('port','dev-access','8085')
fepbase = 'http://' + compose('port','dev-access','8091')
provider_headers = {'X-Mock-PG-Token': secrets['MOCK_PG_TOKEN']}
internal_headers = {'X-Internal-Caller':'acceptance-operator','X-Internal-Service-Token':secrets['INTERNAL_SERVICE_TOKEN']}

def http(method, path, body=None, token=None, headers=None, endpoint=None):
    hs={'Content-Type':'application/json',**(headers or {})}
    if token: hs['Authorization']='Bearer '+token
    if context.get('observability') and method=='POST' and path=='/api/v1/payments':
        trace_id=uuid.uuid4().hex
        report.setdefault('trace_ids',[]).append(trace_id)
        hs['traceparent']='00-'+trace_id+'-'+uuid.uuid4().hex[:16]+'-01'
    data=None if body is None else json.dumps(body).encode()
    req=urllib.request.Request((endpoint or base)+path,data=data,headers=hs,method=method)
    if method=='POST' and path.startswith('/api/v1/payments'):
        report['financial_mutations']+=1
    try:
        with urllib.request.urlopen(req,timeout=20) as response:
            return response.status,json.loads(response.read() or b'{}')
    except urllib.error.HTTPError as error:
        return error.code,json.loads(error.read() or b'{}')

def execute(service, command, sql):
    r=subprocess.run(['docker','exec','-i',project+'-'+service+'-1',*command],input=sql,text=True,capture_output=True,timeout=30)
    if r.returncode: raise RuntimeError('Private database verification failed')
    return r.stdout.strip()
def maria(sql):
    return execute('mariadb',['sh','-c','MYSQL_PWD="$MARIADB_ROOT_PASSWORD" exec mariadb --user=root --batch --raw --skip-column-names ledger_db'],sql)
def pg(database,sql):
    return execute('postgres',['sh','-c','exec psql -X -A -t -U "$POSTGRES_USER" -d "$1" -v ON_ERROR_STOP=1','sh',database],sql)
def scenario(mode):
    status,_=http('POST','/mock/v1/pg/scenarios',{'mode':mode},headers=provider_headers,endpoint=pgbase)
    check('Mock PG scenario '+mode+' configured with authentication',status==200)
def balance(account,token):
    status,body=http('GET',f'/api/v1/accounts/{account}/balances',token=token)
    assert status==200
    return Decimal(str(body['balance']))

def create_payment(key,amount=30000):
    body={'orderReference':key,'merchantId':str(uuid.uuid4()),'amount':amount,'currency':'KRW',
          'sourceAccountId':accounts[0],'destinationAccountId':accounts[1]}
    return body,http('POST','/api/v1/payments',body,tokens[0],{'Idempotency-Key':key})

try:
    tokens=[]; accounts=[]
    for i in range(2):
        email=f'mvp2-{uuid.uuid4().hex}@example.invalid'; password='Test'+uuid.uuid4().hex
        status,_=http('POST','/api/v1/auth/register',{'email':email,'password':password,'name':'MVP2 synthetic','phoneNumber':'00000000000'})
        check('MVP2 synthetic user registered '+str(i),status==201)
        status,body=http('POST','/api/v1/auth/login',{'email':email,'password':password});assert status==200
        tokens.append(body['accessToken'])
        status,body=http('POST','/api/v1/accounts',{'currency':'KRW','alias':'MVP2 synthetic'},tokens[-1]);assert status==201
        accounts.append(str(uuid.UUID(body['accountId'])))
    # Only opening funds are fixtures. Every financial movement below uses real service requests.
    maria("INSERT INTO ledger_accounts(id,external_account_id,balance,currency,version) VALUES "
          +f"('{uuid.uuid4()}','{accounts[0]}',1000000,'KRW',0),('{uuid.uuid4()}','{accounts[1]}',0,'KRW',0);")
    key='approve-'+uuid.uuid4().hex
    request,(status,body)=create_payment(key)
    check('Gateway -> Payment -> FEP -> PG -> Ledger completes synchronously',status==201 and body.get('status')=='APPROVED')
    payment=str(uuid.UUID(body['paymentId']))
    check('Approval debits customer only after confirmed PG and ledger',balance(accounts[0],tokens[0])==970000 and balance(accounts[1],tokens[1])==30000)
    status,again=http('POST','/api/v1/payments',request,tokens[0],{'Idempotency-Key':key})
    check('Same approval request returns original without another PG transmission',status==201 and again.get('paymentId')==payment)
    check('PG received approval mutation once',pg('mock_pg_db',f"select attempts from mock_pg_attempts where operation_id='{payment}';")=='1')
    status,_=http('GET',f'/api/v1/payments/{payment}',token=tokens[1]);check('Other user cannot read payment',status==403)
    status,_=http('POST',f'/api/v1/payments/{payment}/refunds',{'amount':10000},tokens[1],{'Idempotency-Key':'foreign'});check('Other user cannot refund payment',status==403)
    status,refund=http('POST',f'/api/v1/payments/{payment}/refunds',{'amount':10000},tokens[0],{'Idempotency-Key':'partial'})
    check('Partial refund confirms PG and reverse journal before success',status==201 and refund.get('status')=='COMPLETED')
    check('Partial refund returns original account funds',balance(accounts[0],tokens[0])==980000 and balance(accounts[1],tokens[1])==20000)
    status,_=http('POST',f'/api/v1/payments/{payment}/cancel',{},tokens[0]);check('Full cancellation after partial refund is rejected',status==409)
    status,history=http('GET',f'/api/v1/payments/{payment}/refunds',token=tokens[0]);check('Refund history contains the committed refund',status==200 and len(history.get('refunds',[]))==1)
    _,(status,body)=create_payment('cancel-'+uuid.uuid4().hex);assert status==201
    cancelled=str(uuid.UUID(body['paymentId']))
    status,body=http('POST',f'/api/v1/payments/{cancelled}/cancel',{},tokens[0]);check('Full cancellation waits for PG and reverse journal',status==200 and body.get('status')=='CANCELLED')
    check('Full cancellation preserves earlier net payment',balance(accounts[0],tokens[0])==980000 and balance(accounts[1],tokens[1])==20000)
    _,(status,body)=create_payment('concurrent-'+uuid.uuid4().hex);assert status==201
    concurrent=str(uuid.UUID(body['paymentId']))
    def refund_request(index):
        return http('POST',f'/api/v1/payments/{concurrent}/refunds',{'amount':20000},tokens[0],{'Idempotency-Key':f'concurrent-{index}'})[0]
    with ThreadPoolExecutor(max_workers=2) as pool:
        statuses=list(pool.map(refund_request,range(2)))
    check('Concurrent real refund requests cannot exceed approved amount',sorted(statuses)==[201,422])
    scenario('TIMEOUT_AFTER_COMMIT')
    lost_request,(status,lost)=create_payment('lost-'+uuid.uuid4().hex)
    check('PG response loss returns timeout without false internal approval',status==504 and lost.get('paymentId') and lost.get('operationId'))
    lost_id=str(uuid.UUID(lost['paymentId']))
    status,state=http('GET',f'/api/v1/payments/{lost_id}',token=tokens[0]);check('Timed-out approval remains UNKNOWN',status==200 and state.get('status')=='UNKNOWN')
    time.sleep(3)
    status,_=http('POST','/api/v1/payments',lost_request,tokens[0],{'Idempotency-Key':lost_request['orderReference']})
    check('Repeating timed-out request never resends PG approval',status==504 and pg('mock_pg_db',f"select attempts from mock_pg_attempts where operation_id='{lost_id}';")=='1')
    before=balance(accounts[0],tokens[0])
    status,_=http('POST',f'/mock/v1/pg/operations/{lost_id}/webhooks',{'copies':2,'older':False},headers=provider_headers,endpoint=pgbase)
    check('Signed Mock PG -> FEP -> Payment duplicate webhooks converge',status==200)
    status,state=http('GET',f'/api/v1/payments/{lost_id}',token=tokens[0]);check('Webhook confirmation includes successful ledger reflection',status==200 and state.get('status')=='APPROVED')
    check('Duplicate webhook causes one debit',balance(accounts[0],tokens[0])==before-30000)
    check('Lost response and webhook create one journal',maria(f"select count(*) from journals where transaction_id='{lost_id}';")=='1')
    scenario('PENDING')
    _,(status,pending)=create_payment('pending-'+uuid.uuid4().hex)
    check('PG pending is not an internal success',status==504)
    pending_id=str(uuid.UUID(pending['operationId']))
    status,_=http('POST',f'/mock/v1/pg/operations/{pending_id}/confirm',{'outcome':'APPROVED'},headers=provider_headers,endpoint=pgbase);assert status==200
    status,_=http('POST',f'/mock/v1/pg/operations/{pending_id}/webhooks',{'copies':1,'older':False},headers=provider_headers,endpoint=pgbase);check('Final PG callback completes pending operation',status==200)
    before=balance(accounts[0],tokens[0])
    status,_=http('POST',f'/mock/v1/pg/operations/{pending_id}/webhooks',{'copies':1,'older':True},headers=provider_headers,endpoint=pgbase)
    check('Older PG PENDING cannot reverse an already completed effect',status==200 and balance(accounts[0],tokens[0])==before)
    status,_=http('POST','/callbacks/v1/pg',{},headers={'X-PG-Signature':'0'*64},endpoint=fepbase);check('FEP rejects forged original signature',status==400)
    status,_=http('POST','/internal/v1/payments/pg/webhooks',{},headers={'X-PG-Signature':'0'*64},endpoint=paymentbase);check('Payment callback requires internal authentication',status==403)
    status,_=http('POST','/internal/v1/payments/pg/webhooks',{},headers={**internal_headers,'X-Internal-Caller':'fep-adapter','X-PG-Signature':'0'*64},endpoint=paymentbase);check('Payment independently rejects forged provider signature',status==400)
    status,_=http('GET','/mock/v1/pg/results?businessDate='+datetime.now(ZoneInfo('Asia/Seoul')).date().isoformat(),endpoint=pgbase);check('Mock PG access fails closed without provider token',status==403)
    day=datetime.now(ZoneInfo('Asia/Seoul')).date().isoformat()
    def reconcile():
        status,run=http('POST','/internal/v1/payments/reconciliation-runs',{'businessDate':day},headers=internal_headers,endpoint=paymentbase)
        assert status==200 and run['status']=='COMPLETED'
        status,result=http('GET','/internal/v1/payments/reconciliation-runs/'+run['runId'],headers=internal_headers,endpoint=paymentbase)
        assert status==200
        return result
    clean=reconcile()
    check('Three-way reconciliation compares Payment PG and actual MariaDB postings',clean['differences']==[])
    status,_=http('POST','/mock/v1/pg/reconciliation-scenario',{'mode':'AMOUNT_MISMATCH'},headers=provider_headers,endpoint=pgbase);assert status==200
    mismatch=reconcile()
    check('PG file amount discrepancy is persisted with its original reference',any(d['differenceType']=='PG_VALUE' and d['externalReference'] for d in mismatch['differences']))
    check('Reconciliation mismatch and local Outbox event commit together',pg('payment_db',"select count(*) from outbox_events where destination='recon.mismatch';")=='1')
    status,_=http('POST','/mock/v1/pg/reconciliation-scenario',{'mode':'DUPLICATE'},headers=provider_headers,endpoint=pgbase);assert status==200
    check('Duplicate PG file rows are detected',any(d['differenceType']=='PG_DUPLICATE' for d in reconcile()['differences']))
    status,_=http('POST','/mock/v1/pg/reconciliation-scenario',{'mode':'MISSING'},headers=provider_headers,endpoint=pgbase);assert status==200
    check('Missing PG file rows are detected',any(d['differenceType']=='PG_MISSING' for d in reconcile()['differences']))
    http('POST','/mock/v1/pg/reconciliation-scenario',{'mode':'NORMAL'},headers=provider_headers,endpoint=pgbase)
    with urllib.request.urlopen(paymentbase+'/actuator/prometheus',timeout=10) as response:
        text=response.read().decode()
    check('Reconciliation business counter is exported for the existing alert rule','pg_payment_reconciliation_mismatch_total' in text)
    with urllib.request.urlopen(fepbase+'/actuator/prometheus',timeout=10) as response:
        text=response.read().decode()
    check('FEP exports actual provider request latency histogram','pg_fep_adapter_send_duration_seconds_bucket' in text)
    scenario('DECLINE')
    before=balance(accounts[0],tokens[0])
    _,(status,declined)=create_payment('decline-'+uuid.uuid4().hex)
    check('PG final decline never becomes an internal approval',status==502 and declined.get('paymentId'))
    check('PG decline does not move funds',balance(accounts[0],tokens[0])==before)
    # A provider rejection is a completed transport request, not a circuit fault.
    _,(status,adjustable)=create_payment('reserved-'+uuid.uuid4().hex);assert status==201
    adjustable_id=adjustable['paymentId'];scenario('PENDING')
    status,waiting=http('POST',f'/api/v1/payments/{adjustable_id}/refunds',{'amount':20000},tokens[0],{'Idempotency-Key':'reserved-refund'})
    check('Pending refund is not successful and keeps reservation',status==504)
    status,state=http('GET',f'/api/v1/payments/{adjustable_id}',token=tokens[0])
    check('Pending refund reservation is visible in payment query',status==200 and Decimal(str(state['reservedAmount']))==20000)
    status,_=http('POST',f'/api/v1/payments/{adjustable_id}/cancel',{},tokens[0]);check('Cancellation conflicts with pending refund',status==409)
    status,_=http('POST',f'/api/v1/payments/{adjustable_id}/refunds',{'amount':20000},tokens[0],{'Idempotency-Key':'over-reservation'})
    check('Pending reservation counts toward subsequent refund limit',status==422)
    scenario('TIMEOUT_BEFORE_COMMIT')
    before=balance(accounts[0],tokens[0]);request,(status,unanswered)=create_payment('never-processed-'+uuid.uuid4().hex)
    check('Timeout before provider processing remains UNKNOWN',status==504)
    unanswered_id=str(uuid.UUID(unanswered['operationId']));time.sleep(3)
    status,_=http('POST','/api/v1/payments',request,tokens[0],{'Idempotency-Key':request['orderReference']})
    check('Unanswered operation is never sent again',status==504 and pg('mock_pg_db',f"select attempts from mock_pg_attempts where operation_id='{unanswered_id}';")=='1')
    check('No-response operation never debits account',balance(accounts[0],tokens[0])==before)
    # Reset the circuit with a successful transport before testing its exact failure threshold.
    _,(status,_)=create_payment('circuit-reset-'+uuid.uuid4().hex);assert status==201
    for index in range(5):
        scenario('ERROR_BEFORE_COMMIT')
        _,(status,_)=create_payment('breaker-'+uuid.uuid4().hex)
        check('Provider transport failure '+str(index+1)+' is not financial success',status==504)
    attempts=int(pg('mock_pg_db','select sum(attempts) from mock_pg_attempts;'))
    _,(status,_)=create_payment('open-'+uuid.uuid4().hex)
    check('Open circuit refuses a new external financial request',status==503 and int(pg('mock_pg_db','select sum(attempts) from mock_pg_attempts;'))==attempts)
    if context.get('observability'):
        prom='http://'+compose('port','prometheus','9090')
        alert='http://'+compose('port','alertmanager','9093')
        tempo='http://'+compose('port','tempo','3200')
        loki='http://'+compose('port','loki','3100')
        grafana='http://'+compose('port','grafana','3000')
        def fetch(url,headers=None):
            with urllib.request.urlopen(urllib.request.Request(url,headers=headers or {}),timeout=10) as response:
                return json.loads(response.read())
        def eventually(predicate,seconds=90):
            deadline=time.monotonic()+seconds
            while time.monotonic()<deadline:
                try:
                    if predicate():return True
                except (urllib.error.URLError,TimeoutError,ValueError):pass
                time.sleep(2)
            return False
        def query(expression):
            return fetch(prom+'/api/v1/query?'+urllib.parse.urlencode({'query':expression}))['data']['result']
        check('Prometheus scrapes Payment FEP and Mock PG',eventually(lambda:len(query('up{service=~"payment|fep-adapter|mock-pg"} == 1'))==3))
        check('Runtime mismatch counter reaches Prometheus',eventually(lambda:any(float(v['value'][1])>0 for v in query('pg_payment_reconciliation_mismatch_total'))))
        check('Real reconciliation alert reaches local Alertmanager',eventually(lambda:any(a['labels'].get('alertname')=='ReconciliationMismatch' for a in fetch(alert+'/api/v2/alerts')),110))
        trace=report['trace_ids'][0]
        def complete_trace():
            body=fetch(tempo+'/api/traces/'+trace)
            names=set()
            def walk(value):
                if isinstance(value,dict):
                    if value.get('key')=='service.name':
                        text=value.get('value',{})
                        if isinstance(text,dict):names.add(text.get('stringValue') or text.get('string_value'))
                    for child in value.values():walk(child)
                elif isinstance(value,list):
                    for child in value:walk(child)
            walk(body)
            return {'payment','fep-adapter','mock-pg','ledger'}<=names
        check('One real payment trace spans Payment FEP PG and Ledger in Tempo',eventually(complete_trace,90))
        def correlated_log():
            params=urllib.parse.urlencode({'query':'{service="payment"} |= "Payment operation confirmed"','limit':100,
                    'start':str(int((time.time()-900)*1000000000))})
            return trace in json.dumps(fetch(loki+'/loki/api/v1/query_range?'+params))
        check('Real payment completion logs and trace ID are queryable in Loki',eventually(correlated_log,60))
        auth=base64.b64encode(('admin:'+secrets['GRAFANA_ADMIN_PASSWORD']).encode()).decode()
        headers={'Authorization':'Basic '+auth}
        check('Grafana provisions Prometheus Loki and Tempo data sources',eventually(lambda:{'prometheus','loki','tempo'}<={d['type'] for d in fetch(grafana+'/api/datasources',headers)}))
        check('Grafana provisions platform dashboards',len(fetch(grafana+'/api/search?type=dash-db',headers))>=2)
    report['passed']=all(c['passed'] for c in report['checks'])
except Exception as error:
    report['failure']={'type':type(error).__name__,'message':'Inspect isolated private run logs'}
finally:
    save()
    print('MVP2 verdict: '+('PASS' if report['passed'] else 'FAIL'),flush=True)
raise SystemExit(0 if report['passed'] else 1)
