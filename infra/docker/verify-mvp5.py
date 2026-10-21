"""MVP5 acceptance: real service-owned databases and Kafka, local durable Mock institution only."""
import base64
from concurrent.futures import ThreadPoolExecutor
from decimal import Decimal
import hashlib
import hmac
import json
from pathlib import Path
import struct
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid

run = Path(sys.argv[1]).resolve()
context = json.loads((run / 'context.json').read_text())
project = context['project']
if not project.startswith(('pg_mvp5_', 'pg_mvp6_')) or (run / 'mvp5-result.json').exists():
    raise SystemExit('Refusing SWIFT probes outside a fresh isolated MVP5 project')
credentials = json.loads((run / 'swift-test-secrets.json').read_text())
private_env = dict(line.split('=', 1) for line in (run / 'local.env').read_text().splitlines() if '=' in line and not line.startswith('#'))
report = {'passed': False, 'institution': 'LOCAL_DURABLE_MOCK_ONLY', 'checks': []}
def save(): (run / 'mvp5-result.json').write_text(json.dumps(report, ensure_ascii=False, indent=2))
def check(name, value):
    report['checks'].append({'name': name, 'passed': bool(value)}); save()
    print(('PASS ' if value else 'FAIL ') + name, flush=True)
    assert value, name
def compose(*args): return subprocess.check_output(['docker', 'compose', '-f', str(run / 'compose.json'), *args], text=True).strip()
def database(service, db, query):
    command = ['sh', '-c', 'exec psql -X -A -t -U "$POSTGRES_USER" -d "$1" -v ON_ERROR_STOP=1', 'sh', db] if service == 'postgres' else ['sh', '-c', 'MYSQL_PWD="$MARIADB_ROOT_PASSWORD" exec mariadb --user=root --batch --raw --skip-column-names "$1"', 'sh', db]
    result = subprocess.run(['docker', 'compose', '-f', str(run / 'compose.json'), 'exec', '-T', service, *command], input=query, text=True, capture_output=True, timeout=30)
    if result.returncode: raise RuntimeError('Private database assertion failed')
    return result.stdout.strip()
def pg(db, query): return database('postgres', db, query)
def maria(query): return database('mariadb', 'ledger_db', query)
base = 'http://' + compose('port', 'dev-access', '8080')
swift = 'http://' + compose('port', 'dev-access', '8087')
fep = 'http://' + compose('port', 'dev-access', '8091')
mock = 'http://' + compose('port', 'dev-access', '8093')
def request(method, path, body=None, token=None, headers=None, endpoint=None):
    hs = {'Content-Type': 'application/json', **(headers or {})}
    if token: hs['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request((endpoint or base) + path, data=None if body is None else json.dumps(body).encode(), headers=hs, method=method)
    try:
        with urllib.request.urlopen(req, timeout=40) as response: return response.status, json.loads(response.read() or b'{}')
    except urllib.error.HTTPError as e: return e.code, json.loads(e.read() or b'{}')
def internal(caller): return {'X-Internal-Caller': caller, 'X-Internal-Service-Token': credentials['callers'][caller]}
def proof(user, token):
    seed = hmac.new(bytes.fromhex(private_env['AUTH_OTP_SECRET_HEX']), ('otp-seed:' + user).encode(), hashlib.sha256).digest()[:20]
    digest = hmac.new(seed, struct.pack('>Q', int(time.time()) // 30), hashlib.sha1).digest()
    offset = digest[-1] & 15
    code = f"{(struct.unpack('>I', digest[offset:offset + 4])[0] & 0x7fffffff) % 1000000:06d}"
    status, result = request('POST', '/api/v1/auth/otp/verify', {'transferDraftId': str(uuid.uuid4()), 'otpCode': code}, token)
    assert status == 200, 'OTP fixture failed'
    return result['otpToken']
def wait_state(id, token, expected, seconds=45):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        status, value = request('GET', '/api/v1/transfers/overseas/' + id, token=token)
        if status == 200 and value.get('status') in expected: return value
        time.sleep(.5)
    raise AssertionError('Overseas state did not converge: ' + str(value.get('status')))
def count_journals(id): return int(maria("select count(*) from journals where transaction_id in (select '' where false);")) if not id else int(maria("select count(*) from postings p join journals j on j.id=p.journal_id join ledger_accounts a on a.id=p.ledger_account_id where a.external_account_id=(select hold_account_id from overseas_reservations where transfer_id='" + id + "');"))
try:
    users = []
    for index in range(2):
        email = 'swift-' + uuid.uuid4().hex + '@example.test'; password = 'Synthetic-Swift-Pass5!'
        status, registered = request('POST', '/api/v1/auth/register', {'email': email, 'password': password, 'name': 'Synthetic Swift User', 'phoneNumber': '00000000000'})
        check('SWIFT synthetic user registered', status == 201)
        if index == 0:
            admin = context['swift_admin_user_id']
            pg('auth_db', "update users set id='" + admin + "' where id='" + registered['userId'] + "';")
        status, login = request('POST', '/api/v1/auth/login', {'email': email, 'password': password})
        check('Gateway JWT issued for SWIFT user', status == 200)
        token = login['accessToken']; claims = token.split('.')[1]
        actor = json.loads(base64.urlsafe_b64decode(claims + '=' * (-len(claims) % 4)))['sub']
        status, account = request('POST', '/api/v1/accounts', {'currency': 'USD', 'alias': 'Synthetic SWIFT account'}, token)
        check('USD source account opened', status == 201)
        users.append((actor, token, account['accountId']))
    actor, token, source = users[0]; other, other_token, other_source = users[1]
    maria("insert into ledger_accounts(id,external_account_id,balance,currency,version) values ('" + str(uuid.uuid4()) + "','" + source + "',10000,'USD',0),('" + str(uuid.uuid4()) + "','" + other_source + "',1000,'USD',0);")
    def body(recipient='MOCK_SUCCESS', owner=actor, account=source, jwt=token, amount='10.00'):
        return {'fromAccountId': account, 'recipientName': 'PRIVATE SWIFT RECEIVER', 'recipientAccount': recipient, 'bic': 'TESTUS33XXX', 'amount': amount, 'currency': 'USD', 'otpToken': proof(owner, jwt)}
    def submit(recipient='MOCK_SUCCESS', **kwargs):
        b = body(recipient, **kwargs); key = 'swift_' + uuid.uuid4().hex
        status, result = request('POST', '/api/v1/transfers/overseas', b, kwargs.get('jwt', token), {'Idempotency-Key': key})
        if status != 202: report['submission_failure'] = {'http_status': status, 'code': result.get('code')}
        check('Overseas request accepted with durable stable UETR', status == 202 and bool(result.get('uetr')))
        return result, b, key
    good, b, key = submit()
    completed = wait_state(good['transferId'], token, {'COMPLETED'})
    check('Kafka SWIFT FEP Mock and Ledger converge to completion', completed['uetr'] == good['uetr'])
    check('Hold and final journal are separate effects', count_journals(good['transferId']) == 2)
    check('Zero-fee USD invariant debits exactly requested amount', Decimal(maria("select balance from ledger_accounts where external_account_id='" + source + "';")) == Decimal('9990'))
    with ThreadPoolExecutor(max_workers=8) as pool:
        replies = list(pool.map(lambda _: request('POST', '/api/v1/transfers/overseas', b, token, {'Idempotency-Key': key}), range(16)))
    check('Concurrent duplicate requests reuse one transfer and UETR', all(s == 202 and r.get('transferId') == good['transferId'] and r.get('uetr') == good['uetr'] for s, r in replies))
    status, _ = request('POST', '/api/v1/transfers/overseas', {**b, 'amount': '11.00'}, token, {'Idempotency-Key': key})
    check('Same idempotency key with different amount is rejected', status == 409)
    status, _ = request('GET', '/api/v1/transfers/overseas/' + good['transferId'], token=other_token)
    check('Other user cannot query owner transfer', status == 403)
    status, _ = request('GET', '/api/v1/transfers/overseas/' + good['transferId'], token=other_token, headers={'X-User-Id': actor})
    check('Spoofed owner identity cannot bypass overseas isolation', status == 403)
    status, _ = request('GET', '/api/v1/transfers/overseas/' + good['transferId'])
    check('Overseas endpoint requires JWT', status == 401)
    for invalid in [{'currency': 'EUR'}, {'amount': '1.001'}, {'amount': '0'}, {'bic': 'bad'}, {'otpToken': 'forged'}, {'fee': '1.00'}]:
        status, _ = request('POST', '/api/v1/transfers/overseas', {**b, **invalid}, token, {'Idempotency-Key': 'invalid_' + uuid.uuid4().hex})
        check('Invalid currency amount precision BIC or OTP is rejected', status in {400, 401})
    rejected, _, _ = submit('MOCK_REJECT')
    wait_state(rejected['transferId'], token, {'REJECTED'})
    check('External rejection reverses full hold exactly once', count_journals(rejected['transferId']) == 2 and Decimal(maria("select balance from ledger_accounts where external_account_id='" + source + "';")) == Decimal('9990'))
    pending, _, _ = submit('MOCK_PENDING')
    wait_state(pending['transferId'], token, {'PROCESSING'})
    check('Intermediate acceptance does not finalize Ledger', count_journals(pending['transferId']) == 1)
    delayed, _, _ = submit('MOCK_DELAY')
    wait_state(delayed['transferId'], token, {'COMPLETED'})
    check('Delayed final result converges from processing', count_journals(delayed['transferId']) == 2)
    lost, _, _ = submit('MOCK_LOST_RESPONSE')
    wait_state(lost['transferId'], token, {'COMPLETED'})
    check('External processing followed by lost response is recovered by query', count_journals(lost['transferId']) == 2)
    ledger_lost, _, _ = submit('MOCK_SUCCESS', amount='17.00')
    wait_state(ledger_lost['transferId'], token, {'UNKNOWN'})
    faults = json.loads(compose('exec', '-T', 'ledger-loss-proxy', 'python', '-c', "import urllib.request; print(urllib.request.urlopen('http://127.0.0.1:18083/__faults').read().decode())"))
    check('Actual committed Ledger response is disconnected and Transfer remains unknown', faults['dropped_reservation_responses'] >= 1 and count_journals(ledger_lost['transferId']) == 1)
    compose('exec', '-T', 'ledger-loss-proxy', 'python', '-c', "import urllib.request; urllib.request.urlopen(urllib.request.Request('http://127.0.0.1:18083/__faults',data=b'{\"enabled\":false}',headers={'Content-Type':'application/json'},method='POST')).read()")
    compose('restart', 'transfer')
    deadline = time.monotonic() + 80
    while time.monotonic() < deadline:
        try:
            status, _ = request('GET', '/api/v1/transfers/overseas/' + ledger_lost['transferId'], token=token)
            if status == 200: break
        except (urllib.error.URLError, TimeoutError): pass
        time.sleep(1)
    wait_state(ledger_lost['transferId'], token, {'COMPLETED'})
    check('Restart after actual Ledger response loss creates no second debit or hold', count_journals(ledger_lost['transferId']) == 2)
    report['ledger_response_loss'] = 'Actual upstream HTTP 200 after MariaDB commit; downstream socket disconnected, then Transfer restarted and recovered by stable identity'
    unknown, _, _ = submit('MOCK_UNKNOWN')
    time.sleep(2)
    check('Unknown institution outcome preserves hold', count_journals(unknown['transferId']) == 1)
    # Seed the persistent checkpoint produced by a Transfer crash after the real hold commit.
    # This is an explicit recovery fixture, not a claim of dropping an actual TCP response.
    pg('transfer_db', "update overseas_transfers set status='UNKNOWN',lease_until=null,lease_owner=null where id='" + pending['transferId'] + "';")
    compose('restart', 'swift', 'fep-adapter', 'transfer', 'mock-swift')
    deadline = time.monotonic() + 80
    while time.monotonic() < deadline:
        try:
            status, _ = request('GET', '/api/v1/transfers/overseas/' + pending['transferId'], token=token)
            if status == 200: break
        except (urllib.error.URLError, TimeoutError): pass
        time.sleep(1)
    wait_state(pending['transferId'], token, {'PROCESSING'})
    check('Restart recovers seeded post-hold unknown checkpoint without another debit', count_journals(pending['transferId']) == 1)
    retried, _, _ = submit('MOCK_RETRY')
    # Transfer acceptance precedes asynchronous Swift consumption, especially after rebalance.
    deadline = time.monotonic() + 65
    first_observation = None
    while time.monotonic() < deadline:
        observed = pg('swift_db', "select outcome from swift_messages where uetr='" + retried['uetr'] + "';")
        if first_observation is None: first_observation = observed or 'NOT_CONSUMED_YET'
        if observed == 'UNKNOWN': break
        time.sleep(.25)
    check('Retry waits for the actual initial unknown send rather than only Transfer acceptance', observed == 'UNKNOWN')
    report['manual_retry_initial_observation'] = first_observation
    time.sleep(2.2)
    headers = internal('swift-operations')
    status, _ = request('POST', '/api/v1/transfers/overseas/' + retried['transferId'] + '/retransmissions', token=token)
    if status != 200: report['retry_failure'] = {'http_status': status}
    check('Explicit public administrator retransmission option executes', status == 200)
    wait_state(retried['transferId'], token, {'COMPLETED'})
    status, counter = request('GET', '/mock/v1/swift/messages/' + retried['uetr'], headers=headers, endpoint=mock)
    check('Same UETR retry has two deliveries and one Mock financial effect', status == 200 and counter['attempts'] == 2 and counter['financialEffects'] == 1)
    check('Retransmission does not duplicate Ledger finalization', count_journals(retried['transferId']) == 2)
    status, _ = request('POST', '/internal/v1/swift/transfers/' + retried['uetr'] + '/retransmissions', headers=headers, endpoint=swift)
    check('Known final result blocks another actual retransmission', status == 409)
    forged = {'X-Internal-Caller': 'swift-operations', 'X-Internal-Service-Token': private_env['INTERNAL_SERVICE_TOKEN']}
    status, _ = request('GET', '/internal/v1/swift/transfers/' + good['uetr'] + '/messages', headers=forged, endpoint=swift)
    check('Shared financial credential cannot impersonate SWIFT administrator', status == 403)
    status, raw = request('GET', '/api/v1/transfers/overseas/' + good['transferId'] + '/messages', token=token)
    check('Configured administrator can inspect original messages and recipient', status == 200 and 'PRIVATE SWIFT RECEIVER' in str(raw) and 'pacs.008' in str(raw))
    payment_xml = next(item[item.index('<Document'): ] for item in raw if '<Document' in item and 'pacs.008.001.08' in item)
    reference = good['transferId'].replace('-', '')[:16]
    wire = {'transferId': good['transferId'], 'uetr': good['uetr'], 'messageKey': reference, 'pacs008': payment_xml}
    for malformed in [payment_xml.replace('<ChrgBr>SHAR</ChrgBr>', ''), '<!DOCTYPE Document [<!ENTITY private SYSTEM "file:///etc/passwd">]>' + payment_xml.replace('PRIVATE SWIFT RECEIVER', '&private;')]:
        status, error = request('POST', '/internal/v1/fep/swift/messages', {**wire, 'pacs008': malformed}, headers=internal('swift'), endpoint=fep)
        check('FEP rejects schema errors and external entities without exposing original data', status == 400 and all(x not in str(error) for x in ['PRIVATE SWIFT RECEIVER', 'BENEFICIARY', 'passwd']))
    status, _ = request('POST', '/internal/v1/fep/swift/messages', {**wire, 'pacs008': payment_xml.replace('10.00', '11.00')}, headers=internal('swift'), endpoint=fep)
    check('Same UETR with a changed valid XML amount cannot be submitted', status == 409)
    regular, _, _ = submit('MOCK_SUCCESS', owner=other, account=other_source, jwt=other_token)
    wait_state(regular['transferId'], other_token, {'COMPLETED'})
    status, masked = request('GET', '/api/v1/transfers/overseas/' + regular['transferId'] + '/messages', token=other_token)
    check('Ordinary owner receives masked history', status == 200 and 'PRIVATE SWIFT RECEIVER' not in str(masked))
    status, _ = request('POST', '/api/v1/transfers/overseas/' + regular['transferId'] + '/retransmissions', token=other_token, headers={'X-Role': 'ADMIN'})
    check('Ordinary user cannot self-assign administrator role', status == 403)
    status, _ = request('POST', '/mock/v1/swift/messages/' + good['uetr'] + '/results', {'code': 'RJCT'}, headers=headers, endpoint=mock)
    check('Mock injects contradictory final result', status == 200)
    status, _ = request('POST', '/api/v1/transfers/overseas/' + good['transferId'] + '/result-queries', token=token)
    check('Public administrator result-query API executes', status == 200)
    wait_state(good['transferId'], token, {'REVIEW'})
    check('Contradictory final results require review without a third journal', count_journals(good['transferId']) == 2)
    # Restart source owners and durable Mock; existing data belongs only to this project.
    compose('restart', 'swift', 'fep-adapter', 'transfer', 'mock-swift')
    deadline = time.monotonic() + 80
    while time.monotonic() < deadline:
        try:
            status, _ = request('GET', '/api/v1/transfers/overseas/' + retried['transferId'], token=token)
            if status == 200: break
        except (urllib.error.URLError, TimeoutError): pass
        time.sleep(1)
    wait_state(retried['transferId'], token, {'COMPLETED'})
    check('Service restarts preserve final state and journal count', count_journals(retried['transferId']) == 2)
    status, counter = request('GET', '/mock/v1/swift/messages/' + retried['uetr'], headers=headers, endpoint=mock)
    check('Mock process restart preserves UETR deduplication on disk', status == 200 and counter['financialEffects'] == 1 and counter['attempts'] == 2)
    # Exercise retention independently of wall-clock waiting; the rows are synthetic fixtures.
    pg('transfer_db', "update overseas_transfers set private_expires_at=now()-interval '1 second' where id='" + regular['transferId'] + "';update overseas_audit set expires_at=now()-interval '1 second' where transfer_id='" + regular['transferId'] + "';")
    status, expired = request('GET', '/api/v1/transfers/overseas/' + regular['transferId'] + '/messages', token=token)
    check('Expired original messages are unavailable even to administrator', status == 200 and not expired)
    check('Retained financial posting sums remain zero per journal', int(maria('select count(*) from (select journal_id from postings group by journal_id,currency having sum(signed_amount) <> 0) q;')) == 0)
    logs = compose('logs', '--no-color', 'transfer', 'ledger', 'swift', 'fep-adapter', 'mock-swift')
    check('Public logs exclude private recipient and credentials', all(x not in logs for x in ['PRIVATE SWIFT RECEIVER', credentials['mock_token'], *credentials['callers'].values()]))
    private_logs = compose('exec', '-T', 'swift', 'sh', '-c', 'cat /private-logs/data/current.log')
    check('Private administrator log includes original message and recipient', 'PRIVATE SWIFT RECEIVER' in private_logs and 'pacs.008' in private_logs)
    report['passed'] = True
except Exception as error:
    report['failure'] = {'type': type(error).__name__, 'message': str(error) if isinstance(error, AssertionError) else 'Inspect private isolated runner logs'}
    raise
finally: save()
