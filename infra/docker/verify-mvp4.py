"""MVP4 acceptance in disposable pg_mvp4_* only; explanation is explicitly TEST_DOUBLE."""
import argparse
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
import json
from pathlib import Path
import subprocess
import time
import urllib.error
import urllib.request
import uuid

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('run_directory',type=Path)
run=parser.parse_args().run_directory.resolve()
context=json.loads((run/'context.json').read_text())
project=context['project']
if not project.startswith('pg_mvp4_') or (run/'mvp4-result.json').exists():
    raise SystemExit('Refusing report acceptance outside a fresh isolated MVP4 run')
private_env=dict(line.split('=',1) for line in (run/'local.env').read_text().splitlines() if '=' in line and not line.startswith('#'))
report={'passed':False,'checks':[],'explanation_source':'TEST_DOUBLE','real_llm_report_completed':False,'external_llm_calls':0}
def save(): (run/'mvp4-result.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
def check(name,value):
    report['checks'].append({'name':name,'passed':bool(value)});save()
    print(('PASS ' if value else 'FAIL ')+name,flush=True)
    assert value,name
def compose(*args): return subprocess.check_output(['docker','compose','-f',str(run/'compose.json'),*args],text=True).strip()
base='http://'+compose('port','dev-access','8080')
wealth='http://'+compose('port','dev-access','8088')
ledger='http://'+compose('port','dev-access','8083')
def request(method,path,body=None,token=None,headers=None,endpoint=None):
    hs={'Content-Type':'application/json',**(headers or {})}
    if token: hs['Authorization']='Bearer '+token
    req=urllib.request.Request((endpoint or base)+path,data=None if body is None else json.dumps(body).encode(),headers=hs,method=method)
    try:
        with urllib.request.urlopen(req,timeout=110) as response:
            raw=response.read()
            return response.status,raw if response.headers.get_content_type()=='application/pdf' else json.loads(raw or b'{}'),dict(response.headers)
    except urllib.error.HTTPError as e: return e.code,json.loads(e.read() or b'{}'),dict(e.headers)
def sql(database,query):
    result=subprocess.run(['docker','exec','-i',project+'-postgres-1','sh','-c','exec psql -X -A -t -U "$POSTGRES_USER" -d "$1" -v ON_ERROR_STOP=1','sh',database],input=query,text=True,capture_output=True,timeout=30)
    if result.returncode: raise RuntimeError('Private report database check failed')
    return result.stdout.strip()

try:
    users=[]
    for suffix in ['owner','other']:
        email=f'mvp4-{suffix}-{uuid.uuid4().hex[:10]}@example.test';password='Synthetic-Report-Pass4!'
        status,user,_=request('POST','/api/v1/auth/register',{'email':email,'password':password,'name':'합성보고서사용자','phoneNumber':'00000000000'})
        check('Synthetic report user registered',status==201)
        status,login,_=request('POST','/api/v1/auth/login',{'email':email,'password':password})
        check('Report user JWT issued',status==200 and bool(login.get('accessToken')))
        token=login['accessToken'];status,acc,_=request('POST','/api/v1/accounts',{'currency':'KRW','alias':'보고서 합성계좌'},token)
        check('Owned synthetic report account opened',status==201)
        # JWT subject is only decoded for assertions; Gateway validates signature for every call.
        import base64
        payload=token.split('.')[1];subject=json.loads(base64.urlsafe_b64decode(payload+'='*(-len(payload)%4)))['sub']
        users.append((subject,token,acc['accountId']))
    owner,token,account=users[0];other,other_token,other_account=users[1]
    # Synthetic opening funds only; never read or mutate another Docker project's data.
    fixture=f"insert into ledger_accounts(id,external_account_id,balance,currency,version) values('{uuid.uuid4()}','{account}',100000,'KRW',0),('{uuid.uuid4()}','{other_account}',0,'KRW',0);"
    seeded=subprocess.run(['docker','exec','-i',project+'-mariadb-1','sh','-c','MYSQL_PWD="$MARIADB_ROOT_PASSWORD" exec mariadb --user=root --batch ledger_db'],input=fixture,text=True,capture_output=True,timeout=30)
    check('Opening funds exist only in the isolated synthetic fixture',seeded.returncode==0)
    internal={'X-Internal-Caller':'wealth-report','X-Internal-Service-Token':private_env['INTERNAL_SERVICE_TOKEN']}
    status,posted,_=request('POST','/internal/v1/ledger/postings',{'transactionId':str(uuid.uuid4()),'postings':[
        {'accountId':account,'signedAmount':-20000,'currency':'KRW'},
        {'accountId':other_account,'signedAmount':20000,'currency':'KRW'}]},headers=internal,endpoint=ledger)
    check('Real source posting creates a nonzero report balance and period flow',status==200)
    now=datetime.now(timezone.utc)
    cutoff=now.isoformat().replace('+00:00','Z')
    body={'from':(now-timedelta(days=1)).isoformat().replace('+00:00','Z'),'to':cutoff,'asOf':cutoff,'accountIds':[account]}
    key='mvp4-'+uuid.uuid4().hex
    status,result,headers=request('POST','/api/v1/wealth/reports',body,token,{'Idempotency-Key':key})
    report['generation_http_status']=status
    report['generation_error_code']=result.get('code')
    check('Gateway waits for real source collection PDF and DB commit',status==201 and result.get('status')=='COMPLETED')
    check('Explanation provenance cannot be mistaken for real AI',result.get('draftSource')=='TEST_DOUBLE')
    report_id=result['reportId'];url=result['downloadUrl']
    check('Download is a relative authenticated owner path',url==f'/api/v1/wealth/reports/{report_id}/pdf')
    status,pdf,pdf_headers=request('GET',url,token=token)
    check('Owner retrieves an actual private PDF',status==200 and isinstance(pdf,bytes) and pdf.startswith(b'%PDF-') and len(pdf)>1000)
    check('PDF caching is disabled',pdf_headers.get('Cache-Control')=='no-store')
    (run/'synthetic-wealth-report.pdf').write_bytes(pdf)
    for suffix in ['', '/pdf']:
        status,_,_=request('GET',f'/api/v1/wealth/reports/{report_id}'+suffix,token=other_token)
        check('Other user cannot access report'+suffix,status==404)
    status,_,_=request('GET',url)
    check('Download requires JWT',status==401)
    status,_,_=request('GET',url,headers={'X-User-Id':owner},token=other_token)
    check('Spoofed owner header is removed by Gateway',status==404)
    status,page,_=request('GET','/api/v1/wealth/reports',token=other_token)
    check('Other user list omits owner report',status==200 and all(r['reportId']!=report_id for r in page['reports']))
    status,replay,_=request('POST','/api/v1/wealth/reports',body,token,{'Idempotency-Key':key})
    check('Same request reuses completed receipt without generation',status==201 and replay.get('reportId')==report_id)
    changed={**body,'accountIds':[other_account]}
    status,_,_=request('POST','/api/v1/wealth/reports',changed,token,{'Idempotency-Key':key})
    check('Conflicting payload cannot reuse idempotency key',status==409)
    status,_,_=request('POST','/api/v1/wealth/reports',changed,token,{'Idempotency-Key':'foreign-'+uuid.uuid4().hex})
    check('Source service verifies account ownership before collecting',status==403)
    for bad in [{**body,'from':body['to']},{**body,'accountIds':[account,account]},{**body,'actorUserId':other}]:
        status,_,_=request('POST','/api/v1/wealth/reports',bad,token,{'Idempotency-Key':uuid.uuid4().hex})
        check('Period duplicate-account or identity-body input is rejected',status==400)
    internal={'X-Internal-Caller':'wealth-report','X-Internal-Service-Token':private_env['INTERNAL_SERVICE_TOKEN']}
    status,snap,_=request('POST','/internal/v1/ledger/report-snapshots',{'userId':owner,**body},headers=internal,endpoint=ledger)
    check('Actual Ledger snapshot contract returns a complete committed read',status==200 and len(snap.get('accounts',[]))==1 and snap['userId']==owner)
    status,_,_=request('POST','/internal/v1/ledger/report-snapshots',{'userId':owner,**body},endpoint=ledger)
    check('Internal source rejects unauthenticated calls',status==403)
    status,_,_=request('POST','/internal/v1/ledger/report-snapshots',{'userId':owner,**body},headers={**internal,'X-Internal-Caller':'payment'},endpoint=ledger)
    check('Authenticated non-report caller is forbidden',status==403)
    check('PostgreSQL stores frozen source and PDF rows',sql('wealth_report_db',f"select count(*) from asset_snapshots s join snapshot_accounts a on a.snapshot_id=s.id join report_files f on f.report_id=s.report_id where s.report_id='{report_id}';")=='1')
    check('Committed source values and exact report calculation match',sql('wealth_report_db',f"select balance::text||'|'||cash_out::text from snapshot_accounts where account_id='{account}';")=='80000|20000')
    # Real Kafka records from MVP1/MVP2 are consumed through their existing contract.
    deadline=time.monotonic()+20
    while time.monotonic()<deadline and sql('wealth_report_db','select count(*) from snapshot_effects;')=='0': time.sleep(.5)
    check('Actual Kafka completion events update separate durable projection',int(sql('wealth_report_db','select count(*) from snapshot_effects;'))>0)
    check('Cross-topic events share one financial effect identity',sql('wealth_report_db','select count(*) from (select transaction_id,count(distinct topic) n from snapshot_events group by transaction_id having count(distinct topic)>1) q;')!='0')
    check('Events never auto-generate a report',sql('wealth_report_db','select count(*) from asset_reports;')=='2')
    # Exercise actual storage failure; the successful receipt and its PDF must remain intact.
    sql('wealth_report_db','alter table report_files add constraint injected_pdf_failure check(false) not valid;')
    try:
        status,failed,_=request('POST','/api/v1/wealth/reports',body,token,{'Idempotency-Key':'db-failure-'+uuid.uuid4().hex})
        check('Actual PDF storage failure returns safe 503',status==503 and failed.get('code')=='REPORT-STORAGE_FAILED')
    finally: sql('wealth_report_db','alter table report_files drop constraint injected_pdf_failure;')
    check('Storage failure cannot publish or corrupt a completed PDF',sql('wealth_report_db','select count(*) from report_files;')=='1')
    interrupted_id=str(uuid.uuid4())
    sql('wealth_report_db',f"insert into asset_reports(id,user_id,request_key,fingerprint,as_of,status,stage,calculation_version,deadline) values('{interrupted_id}','{owner}','interrupted-fixture','fixture','{cutoff}','PROCESSING','DRAFTING','krw-deposit-gross-v1',clock_timestamp()-interval '1 second');")
    compose('restart','wealth-report')
    until=time.monotonic()+65
    recovered=''
    while time.monotonic()<until:
        recovered=sql('wealth_report_db',f"select status||'|'||stage from asset_reports where id='{interrupted_id}';")
        if recovered=='INTERRUPTED|DRAFTING': break
        time.sleep(1)
    check('Actual process restart detects an expired drafting attempt without replay',recovered=='INTERRUPTED|DRAFTING')
    check('Restart recovery cannot create another PDF or model-completion receipt',sql('wealth_report_db','select count(*) from report_files;')=='1')
    columns=sql('wealth_report_db',"select column_name from information_schema.columns where table_schema='public';")
    check('Active report schema stores no raw prompt or restoration mapping',all(x not in columns for x in ['protected_original_value','token_label','prompt','account_number','password']))
    logs=compose('logs','--no-color','wealth-report')
    check('Report logs contain no synthetic name email credential or original prompt',all(x not in logs for x in ['합성보고서사용자','Synthetic-Report-Pass4!',private_env['INTERNAL_SERVICE_TOKEN'],private_env['AI_PREPARATION_WEALTH_TOKEN']]))
    status,_,_=request('GET',url,token=token)
    check('Previously completed report remains downloadable after failures',status==200)
    report['passed']=all(c['passed'] for c in report['checks'])
except Exception as error:
    report['failure_type']=type(error).__name__
finally: save()
raise SystemExit(0 if report['passed'] else 1)
