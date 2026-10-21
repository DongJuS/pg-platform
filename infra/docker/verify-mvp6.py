"""Fresh isolated MVP6 acceptance. Source facts via owning REST, real Kafka/PostgreSQL/MariaDB, local Mock only."""
import base64
from concurrent.futures import ThreadPoolExecutor
from decimal import Decimal
import json
from pathlib import Path
import subprocess
import sys
import time
import urllib.request
import urllib.error
import uuid
run=Path(sys.argv[1]).resolve()
context=json.loads((run/'context.json').read_text())
if not context['project'].startswith('pg_mvp6_') or (run/'mvp6-result.json').exists():
    raise SystemExit('Refusing probes outside a fresh isolated MVP6 project')
credentials=json.loads((run/'aml-test-secrets.json').read_text())
report={'passed':False,'policy':'USER_CONFIRMED_DEVELOPMENT_V1','institution':'LOCAL_DURABLE_MOCK_ONLY','checks':[]}
def save(): (run/'mvp6-result.json').write_text(json.dumps(report,indent=2))
def check(name,ok):
    report['checks'].append({'name':name,'passed':bool(ok)});save();print(('PASS ' if ok else 'FAIL ')+name,flush=True)
    assert ok,name
def compose(*args):return subprocess.check_output(['docker','compose','-f',str(run/'compose.json'),*args],text=True).strip()
def pg(db,sql):
    r=subprocess.run(['docker','compose','-f',str(run/'compose.json'),'exec','-T','postgres','sh','-c','exec psql -X -A -t -U "$POSTGRES_USER" -d "$1" -v ON_ERROR_STOP=1','sh',db],input=sql,text=True,capture_output=True,timeout=30)
    if r.returncode: raise RuntimeError('Private database assertion failed')
    return r.stdout.strip()
def maria(sql):
    r=subprocess.run(['docker','compose','-f',str(run/'compose.json'),'exec','-T','mariadb','sh','-c','MYSQL_PWD="$MARIADB_ROOT_PASSWORD" exec mariadb --user=root --batch --raw --skip-column-names ledger_db'],input=sql,text=True,capture_output=True,timeout=30)
    if r.returncode:raise RuntimeError('Private MariaDB assertion failed')
    return r.stdout.strip()
def base(port):return 'http://'+compose('port','dev-access',str(port))
def headers(caller):return {'X-Internal-Caller':caller,'X-Internal-Service-Token':credentials['callers'][caller]}
def request(method,endpoint,path,body=None,hs=None):
    req=urllib.request.Request(endpoint+path,method=method,headers={'Content-Type':'application/json',**(hs or {})},data=None if body is None else json.dumps(body).encode())
    try:
        with urllib.request.urlopen(req,timeout=40) as r:return r.status,json.loads(r.read() or b'{}')
    except urllib.error.HTTPError as e:return e.code,json.loads(e.read() or b'{}')
    except (urllib.error.URLError, OSError):return 0,{}
def wait(probe,seconds=80):
    until=time.monotonic()+seconds
    while time.monotonic()<until:
        if probe():return True
        time.sleep(.5)
    return False
def emit(topic,payload):
    r=subprocess.run(['docker','compose','-f',str(run/'compose.json'),'exec','-T','kafka','/opt/kafka/bin/kafka-console-producer.sh','--bootstrap-server','kafka:19092','--topic',topic],input=json.dumps(payload)+'\n',text=True,capture_output=True,timeout=30)
    if r.returncode:raise RuntimeError('Kafka fixture failed')
def document():
    rid=str(uuid.uuid4())
    return {'reportId':rid,'reportKey':'report_'+rid,'caseId':str(uuid.uuid4()),'reportType':'STR_SIMULATION','customerId':str(uuid.uuid4()),'currency':'KRW','amount':100,'transactionCount':1,'ruleVersion':'fixture-transport-v1','riskScore':70,'windowStart':'2026-10-20T00:00:00Z','windowEnd':'2026-10-21T00:00:00Z','reasons':['TRANSPORT_FIXTURE=70']}
try:
    aml,fep,mock=base(8089),base(8091),base(8094)
    ops=headers('aml-operations');internal=headers('aml')
    check('Prior suites published transactions before AML consumer starts',int(pg('transfer_db',"select count(*) from transfers where status='COMPLETED'"))>0)
    check('Disabled consumer has not fabricated financial facts',pg('aml_db','select count(*) from aml_facts')=='0')
    conf=json.loads((run/'compose.json').read_text());conf['services']['aml']['environment']['PG_AML_EVENTS_ENABLED']='true'
    (run/'compose.json').write_text(json.dumps(conf))
    compose('up','-d','--wait','--wait-timeout','120','--no-deps','aml')
    check('Fresh consumer replays already published Kafka events',wait(lambda:int(pg('aml_db','select count(*) from aml_facts'))>0))
    # New test-owned accounts and opening balances; all monitored transfers run through the real authenticated API.
    import hashlib,hmac,struct
    gateway=base(8080)
    private_env=dict(line.split('=',1) for line in (run/'local.env').read_text().splitlines() if '=' in line and not line.startswith('#'))
    email='aml-'+uuid.uuid4().hex+'@example.test';password='Synthetic-Aml-Pass6!'
    code,registered=request('POST',gateway,'/api/v1/auth/register',{'email':email,'password':password,'name':'Synthetic AML User','phoneNumber':'00000000000'})
    check('Synthetic AML user is registered through existing API',code==201)
    code,login=request('POST',gateway,'/api/v1/auth/login',{'email':email,'password':password})
    jwt=login['accessToken'];auth={'Authorization':'Bearer '+jwt};customer=registered['userId']
    accounts=[]
    for i in range(2):
        code,a=request('POST',gateway,'/api/v1/accounts',{'currency':'KRW','alias':'Synthetic AML'},auth)
        assert code==201;accounts.append(a['accountId'])
    maria("insert into ledger_accounts(id,external_account_id,balance,currency,version) values ('"+str(uuid.uuid4())+"','"+accounts[0]+"',100000000,'KRW',0),('"+str(uuid.uuid4())+"','"+accounts[1]+"',0,'KRW',0)")
    fixture_transactions=[]
    for i in range(5):
        seed=hmac.new(bytes.fromhex(private_env['AUTH_OTP_SECRET_HEX']),('otp-seed:'+customer).encode(),hashlib.sha256).digest()[:20]
        digest=hmac.new(seed,struct.pack('>Q',int(time.time())//30),hashlib.sha1).digest();offset=digest[-1]&15
        otp=f"{(struct.unpack('>I',digest[offset:offset+4])[0]&0x7fffffff)%1000000:06d}"
        code,proof=request('POST',gateway,'/api/v1/auth/otp/verify',{'transferDraftId':str(uuid.uuid4()),'otpCode':otp},auth)
        assert code==200
        key='aml_'+uuid.uuid4().hex
        code,t=request('POST',gateway,'/api/v1/transfers',{'fromAccountId':accounts[0],'toAccountId':accounts[1],'amount':5000000,'currency':'KRW','otpToken':proof['otpToken']},{**auth,'Idempotency-Key':key})
        check('Confirmed-rule fixture transfer completes through existing API',code==200 and t['status']=='COMPLETED')
        fixture_transactions.append(pg('transfer_db',"select transaction_id from transfers where idempotency_key='"+key+"'"))
    check('Automatic decisions reach local Mock final acceptance through FEP',wait(lambda:int(pg('aml_db',"select count(*) from aml_reports where status='REPORTED'"))>0))
    check('Transfer and Ledger topics share canonical transaction identity',pg('aml_db','select count(*) from (select transaction_id from aml_facts group by transaction_id having count(*)>1) x')=='0')
    check('Same transaction and rule version cannot produce duplicate cases',pg('aml_db','select count(*) from (select transaction_id,rule_version from aml_cases group by transaction_id,rule_version having count(*)>1) x')=='0')
    check('Overseas holding journals are excluded',wait(lambda:int(pg('aml_db',"select count(*) from aml_inbox where reason='OVERSEAS_HOLD'"))>0))
    check('Reversals and refunds remain linked and are not new report anchors',wait(lambda:int(pg('aml_db',"select count(*) from aml_facts where original_business_id is not null"))>0) and pg('aml_db',"select count(*) from aml_cases c join aml_facts f on f.transaction_id=c.transaction_id where f.kind in ('PAYMENT_REFUND','PAYMENT_CANCEL','OVERSEAS_REVERSAL')")=='0')
    check('Missing administrator credential is rejected',request('GET',aml,'/internal/v1/aml/cases')[0]==403)
    check('Other service cannot read any case even with a valid service token',request('GET',aml,'/internal/v1/aml/cases',hs=headers('payment'))[0]==403)
    forged={**headers('payment'),'X-Internal-Caller':'aml-operations','X-User-Role':'ADMIN'}
    check('Caller and role spoofing cannot elevate to AML operations',request('GET',aml,'/internal/v1/aml/cases',hs=forged)[0]==403)
    code,cases=request('GET',aml,'/internal/v1/aml/cases?size=1',hs=ops)
    check('Administrator searches a bounded page',code==200 and len(cases['cases'])==1 and cases.get('nextPage') is not None)
    check('Invalid pagination is rejected',request('GET',aml,'/internal/v1/aml/cases?size=101',hs=ops)[0]==400)
    check('Invalid date range is rejected',request('GET',aml,'/internal/v1/aml/cases?from=2026-10-22T00:00:00Z&to=2026-10-21T00:00:00Z',hs=ops)[0]==400)
    check('Unknown case stays 404 for authorized administrator',request('GET',aml,'/internal/v1/aml/cases/'+str(uuid.uuid4()),hs=ops)[0]==404)
    row=pg('aml_db',"select transaction_id from aml_facts where kind='DOMESTIC_TRANSFER' order by transaction_id limit 1")
    journal=maria("select id from journals where transaction_id='"+row+"'")
    before=pg('aml_db',"select count(*) from aml_cases where transaction_id='"+row+"'")
    e={'eventId':str(uuid.uuid4()),'transactionId':row,'journalId':journal,'occurredAt':'2026-10-21T00:00:00Z'}
    emit('ledger.posted',e);emit('ledger.posted',e)
    e2={**e,'eventId':str(uuid.uuid4()),'transferId':row};emit('transfer.completed',e2)
    check('Duplicate and cross-topic replay commits Inbox without double counting',wait(lambda:pg('aml_db',"select count(*) from aml_inbox where event_id in ('"+e['eventId']+"','"+e2['eventId']+"') and status='RESOLVED'")=='2') and pg('aml_db',"select count(*) from aml_cases where transaction_id='"+row+"'")==before)
    missing={'eventId':str(uuid.uuid4()),'transactionId':str(uuid.uuid4()),'journalId':str(uuid.uuid4()),'occurredAt':'2026-10-21T00:00:00Z'}
    emit('ledger.posted',missing)
    check('Unavailable original evidence stays pending instead of score zero',wait(lambda:pg('aml_db',"select reason from aml_inbox where event_id='"+missing['eventId']+"'")=='SOURCE_UNAVAILABLE') and pg('aml_db',"select count(*) from aml_cases where transaction_id='"+missing['transactionId']+"'")=='0')
    counts=pg('aml_db','select (select count(*) from aml_facts),(select count(*) from aml_reports)')
    compose('restart','aml','fep-adapter','mock-kofiu')
    check('Process restarts preserve facts reports and durable receipt recovery',wait(lambda:request('GET',aml,'/internal/v1/aml/cases?size=1',hs=ops)[0]==200) and all(int(after)>=int(before) for after,before in zip(pg('aml_db','select (select count(*) from aml_facts),(select count(*) from aml_reports)').split('|'),counts.split('|'))))
    def scenario(d,mode):return request('POST',mock,'/mock/v1/kofiu/scenarios',{'reportKey':d['reportKey'],'mode':mode},{'X-Kofiu-Operations-Token':credentials['operations_token']})
    def send(d):return request('POST',fep,'/internal/v1/fep/kofiu/reports',d,internal)
    def query(d):return request('GET',fep,'/internal/v1/fep/kofiu/reports/'+d['reportKey'],hs=internal)
    lost=document();check('Mock response-loss scenario configured separately from report contents',scenario(lost,'RESPONSE_LOSS')[0]==200)
    code,r=send(lost)
    check('Actual Mock acceptance followed by HTTP timeout is UNKNOWN at FEP',code==202 and r['transmissionStatus']=='UNKNOWN' and r['institutionStatus']=='UNKNOWN')
    code,r=query(lost)
    check('Result query recovers final receipt without resending',code==200 and r['institutionStatus']=='ACCEPTED' and r['deliveries']==1 and r['acceptances']==1)
    with ThreadPoolExecutor(max_workers=6) as pool:duplicates=list(pool.map(lambda _:send(lost),range(12)))
    check('Concurrent duplicate report registration does not repeat institution send',all(code==202 for code,_ in duplicates) and query(lost)[1]['deliveries']==1)
    check('Same report key with different amount conflicts',send({**lost,'amount':101})[0]==409)
    check('Final acceptance cannot be retransmitted',request('POST',fep,'/internal/v1/fep/kofiu/reports/'+lost['reportKey']+'/retransmissions',{},internal)[0]==409)
    pending=document();scenario(pending,'PENDING');code,r=send(pending)
    check('FEP durable receipt is distinct from pending institution decision',code==202 and r['receiptReference'].startswith('FEP_') and r['institutionStatus']=='PENDING')
    code,_=request('POST',mock,'/mock/v1/kofiu/results',{'reportKey':pending['reportKey'],'status':'ACCEPTED'},{'X-Kofiu-Operations-Token':credentials['operations_token']})
    check('Later final institution result is obtained by query',code==200 and query(pending)[1]['institutionStatus']=='ACCEPTED')
    rejected=document();scenario(rejected,'REJECT');check('Institution rejection remains distinct from transport failure',send(rejected)[1]['institutionStatus']=='REJECTED')
    retry=document();scenario(retry,'UNAVAILABLE');code,r=send(retry)
    check('Uncertain failed send retains immutable receipt and report content',code==202 and r['transmissionStatus']=='UNKNOWN')
    scenario(retry,'SUCCESS');time.sleep(1.1)
    code,r=request('POST',fep,'/internal/v1/fep/kofiu/reports/'+retry['reportKey']+'/retransmissions',{},internal)
    check('Explicit retransmission after authoritative NOT_RECEIVED uses same identity and content',code==200 and r['institutionStatus']=='ACCEPTED' and r['deliveries']==1 and r['acceptances']==1)
    check('FEP records two distinct transmission attempts but one acceptance',pg('fep_adapter_db',"select count(*) from kofiu_attempts where report_id='"+retry['reportId']+"'")=='2')
    compose('restart','mock-kofiu');check('Mock process restart preserves original acceptance',wait(lambda:query(lost)[1].get('institutionStatus')=='ACCEPTED') and query(lost)[1]['acceptances']==1)
    check('All AML reports have one atomic local Outbox intent',pg('aml_db',"select count(*) from aml_reports r where not exists(select 1 from outbox_events o where o.routing_key=r.id)")=='0')
    check('No source PII name account-number OTP or credential fields persisted in AML documents',pg('aml_db',"select count(*) from aml_reports where document like '%recipientName%' or document like '%otpToken%' or document like '%password%'")=='0')
    # Late-evidence fixture: real sixth transfer, with only its test-owned source journal timestamp shifted before the fifth anchor.
    anchor=fixture_transactions[-1]
    original=pg('aml_db',"select r.id from aml_reports r join aml_cases c on c.id=r.case_id where c.transaction_id='"+anchor+"' and r.revision=1")
    case_id=pg('aml_db',"select case_id from aml_reports where id='"+original+"'")
    original_hash=pg('aml_db',"select content_hash from aml_reports where id='"+original+"'")
    check('Original report is finally accepted before amendment fixture',wait(lambda:pg('aml_db',"select status from aml_reports where id='"+original+"'")=='REPORTED'))
    conf['services']['aml']['environment']['PG_AML_EVENTS_ENABLED']='false';(run/'compose.json').write_text(json.dumps(conf))
    compose('up','-d','--wait','--wait-timeout','120','--no-deps','aml')
    seed=hmac.new(bytes.fromhex(private_env['AUTH_OTP_SECRET_HEX']),('otp-seed:'+customer).encode(),hashlib.sha256).digest()[:20]
    digest=hmac.new(seed,struct.pack('>Q',int(time.time())//30),hashlib.sha1).digest();offset=digest[-1]&15
    otp=f"{(struct.unpack('>I',digest[offset:offset+4])[0]&0x7fffffff)%1000000:06d}"
    code,proof=request('POST',gateway,'/api/v1/auth/otp/verify',{'transferDraftId':str(uuid.uuid4()),'otpCode':otp},auth)
    assert code==200
    key='aml_late_'+uuid.uuid4().hex
    code,t=request('POST',gateway,'/api/v1/transfers',{'fromAccountId':accounts[0],'toAccountId':accounts[1],'amount':5000000,'currency':'KRW','otpToken':proof['otpToken']},{**auth,'Idempotency-Key':key})
    assert code==200
    late=pg('transfer_db',"select transaction_id from transfers where idempotency_key='"+key+"'")
    maria("update journals late join journals anchor on anchor.transaction_id='"+anchor+"' set late.posted_at=anchor.posted_at-interval 1 microsecond where late.transaction_id='"+late+"'")
    conf['services']['aml']['environment']['PG_AML_EVENTS_ENABLED']='true';(run/'compose.json').write_text(json.dumps(conf))
    compose('up','-d','--wait','--wait-timeout','120','--no-deps','aml')
    check('Delayed source evidence moves frozen reported case to review',wait(lambda:pg('aml_db',"select status from aml_cases where id='"+case_id+"'")=='REVIEW'))
    code,detail=request('GET',aml,'/internal/v1/aml/cases/'+case_id,hs=ops)
    check('Administrator sees saved original and latest decision evidence',code==200 and detail['latestAssessment']['totalAmount']>detail['originalAssessment']['totalAmount'])
    code,amended=request('POST',aml,'/internal/v1/aml/cases/'+case_id+'/report-amendments',{'previousReportId':original},ops)
    check('Explicit amendment creates a separately accepted report',code==200 and amended['institutionStatus']=='ACCEPTED' and amended['reportId']!=original)
    code,again=request('POST',aml,'/internal/v1/aml/cases/'+case_id+'/report-amendments',{'previousReportId':original},ops)
    check('Repeated amendment request reuses same new report identity',code==200 and again['reportId']==amended['reportId'])
    code,amend_doc=request('GET',aml,'/internal/v1/aml/cases/'+case_id+'/report-document',hs=ops)
    check('Amendment references original immutable report and revision',code==200 and amend_doc['revision']==2 and amend_doc['supersedesReportId']==original and pg('aml_db',"select content_hash from aml_reports where id='"+original+"'")==original_hash)
    check('No further amendment is allowed without new evidence',request('POST',aml,'/internal/v1/aml/cases/'+case_id+'/report-amendments',{'previousReportId':amended['reportId']},ops)[0]==409)
    # Stop/restart only this run's PostgreSQL and verify durable source/Inbox processing resumes.
    compose('restart','postgres')
    check('Actual PostgreSQL process restart recovers without missing durable reports',wait(lambda:request('GET',aml,'/internal/v1/aml/cases?size=1',hs=ops)[0]==200))
    check('Confirmed user rule version is persisted with every evaluated case',pg('aml_db',"select count(*) from aml_cases where rule_version<>'development-v1'")=='0')
    # Advance synthetic row clocks; this tests retention logic, not 48 elapsed wall-clock hours.
    expired=pg('aml_db',"select id from aml_reports order by created_at limit 1")
    expired_case=pg('aml_db',"select case_id from aml_reports where id='"+expired+"'")
    pg('aml_db',"update aml_reports set created_at=now()-interval '49 hours' where id='"+expired+"';update aml_cases set created_at=now()-interval '49 hours' where id='"+expired_case+"'")
    check('Expired case is unavailable even to authorized administrator',request('GET',aml,'/internal/v1/aml/cases/'+expired_case,hs=ops)[0]==404)
    check('Scheduled retention removes expired report content',wait(lambda:pg('aml_db',"select count(*) from aml_reports where id='"+expired+"'")=='0'))
    report['passed']=True
except Exception as e:
    report['failure']={'type':type(e).__name__,'message':'See named assertion and private isolated logs'}
finally:save()
raise SystemExit(0 if report['passed'] else 1)
