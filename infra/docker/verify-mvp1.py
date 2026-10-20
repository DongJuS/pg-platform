import json
from decimal import Decimal
from pathlib import Path
import subprocess
import time
import urllib.error
import urllib.request
import uuid
from datetime import datetime
from zoneinfo import ZoneInfo

import argparse, base64, hashlib, hmac, struct
parser = argparse.ArgumentParser(description="MVP1 acceptance checks; only isolated pg_mvp1_* projects are allowed")
parser.add_argument("run_directory", type=Path)
ROOT = parser.parse_args().run_directory.resolve()
if (ROOT / "result.json").exists():
    previous = json.loads((ROOT / "result.json").read_text())
    if previous.get("financial_post_requests", 0):
        raise SystemExit("A financial POST was already issued. Inspect its existing transaction; do not rerun.")
CONTEXT = json.loads((ROOT / "context.json").read_text())
PROJECT = CONTEXT["project"]
if not PROJECT.startswith(("pg_mvp1_", "pg_mvp2_", "pg_mvp3_", "pg_mvp4_")):
    raise SystemExit("Refusing to run financial acceptance probes outside an isolated pg_mvp1_* project")
private_env = dict(line.split("=",1) for line in (ROOT / "local.env").read_text().splitlines() if "=" in line and not line.startswith("#"))
OTP_SECRET = bytes.fromhex(private_env["AUTH_OTP_SECRET_HEX"])
INTERNAL_TOKEN = private_env["INTERNAL_SERVICE_TOKEN"]
REPORT = {
    "started_at_kst": datetime.now(ZoneInfo("Asia/Seoul")).isoformat(),
    "context": CONTEXT,
    "preconditions": {
        "account_proxy_workaround": False,
        "opening_balance_fixture_only": True,
        "otp_verified": True,
        "existing_stack_isolated": True,
    },
    "amount": "10000", "currency": "KRW", "financial_post_requests": 0,
    "checks": [],
}

def save():
    (ROOT / "result.json").write_text(json.dumps(REPORT, ensure_ascii=False, indent=2))

def check(name, passed, actual=None):
    REPORT["checks"].append({"name": name, "passed": bool(passed), "actual": actual})
    save()
    print(("PASS " if passed else "FAIL ") + name, flush=True)

def compose(*args):
    return subprocess.check_output(["docker", "compose", "-f", str(ROOT / "compose.json"), *args], text=True)

def execute(service, command, sql=None, timeout=40):
    result = subprocess.run(["docker", "exec", "-i", PROJECT + "-" + service + "-1", *command],
                            input=sql, text=True, capture_output=True, timeout=timeout)
    if result.returncode:
        raise RuntimeError(service + " verification command failed; inspect private diagnostic logs")
    return result.stdout

def pg(database, query):
    sql = "SELECT COALESCE(json_agg(q), '[]'::json)::text FROM (" + query + ") q;"
    output = execute("postgres", ["sh", "-c", 'exec psql -X -A -t -U "$POSTGRES_USER" -d "$1" -v ON_ERROR_STOP=1', "sh", database], sql)
    return json.loads(output)

def maria(database, sql):
    return execute("mariadb", ["sh", "-c", 'MYSQL_PWD="$MARIADB_ROOT_PASSWORD" exec mariadb --user=root --batch --raw --skip-column-names "$1"', "sh", database], sql)

def maria_rows(database, select):
    return [json.loads(line) for line in maria(database, select).splitlines() if line.strip()]

BASE = "http://" + compose("port", "dev-access", "8080").strip()
REPORT["gateway_address"] = BASE

def http(method, path, body=None, token=None, headers=None, base=None):
    request_headers = {"Content-Type": "application/json", **(headers or {})}
    if token:
        request_headers["Authorization"] = "Bearer " + token
    request = urllib.request.Request((base or BASE) + path, method=method, headers=request_headers,
                                     data=None if body is None else json.dumps(body).encode())
    if method == "POST" and path == "/api/v1/transfers":
        REPORT["financial_post_requests"] += 1
        save()
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return response.status, json.loads(response.read() or b"{}"), response.headers.get("X-Trace-Id")
    except urllib.error.HTTPError as error:
        return error.code, json.loads(error.read() or b"{}"), error.headers.get("X-Trace-Id")
    except (urllib.error.URLError, TimeoutError) as error:
        return 0, {"transportError": type(error).__name__}, None

def issue_proof(index):
    seed = hmac.new(OTP_SECRET, ("otp-seed:" + users[index]).encode(), hashlib.sha256).digest()[:20]
    digest = hmac.new(seed, struct.pack(">Q", int(time.time()) // 30), hashlib.sha1).digest()
    offset = digest[-1] & 15
    code = f"{(struct.unpack('>I', digest[offset:offset+4])[0] & 0x7fffffff) % 1000000:06d}"
    status, body, _ = http("POST", "/api/v1/auth/otp/verify", {"transferDraftId": str(uuid.uuid4()), "otpCode": code}, tokens[index])
    check("Real Mock-TOTP authorization issued", status == 200, status)
    assert status == 200, "OTP authorization failed"
    return body["otpToken"]

def reject(name, method, path, expected, body=None, token=None, headers=None, base=None):
    status, result, _ = http(method, path, body, token, headers, base)
    check(name, status == expected, {"status": status, "code": result.get("code")})
    return result

def balance(account, token):
    status, body, _ = http("GET", f"/api/v1/accounts/{account}/balances", token=token)
    assert status == 200, "Balance read failed with HTTP " + str(status)
    return str(body["balance"])

def ledger_outbox():
    return maria_rows("ledger_db", "SELECT JSON_OBJECT('eventId',event_id,'destination',destination,'routingKey',routing_key,'payload',CONVERT(payload USING utf8mb4),'publishedAt',CAST(published_at AS CHAR),'attempts',attempts) FROM outbox_events ORDER BY created_at;")

def transfer_outbox():
    return pg("transfer_db", "SELECT event_id AS \"eventId\",destination,routing_key AS \"routingKey\",convert_from(payload,'UTF8') AS payload,published_at AS \"publishedAt\",attempts FROM outbox_events ORDER BY created_at")

def kafka(*args):
    return execute("kafka", ["env", "KAFKA_HEAP_OPTS=-Xms32m -Xmx96m", *args], timeout=45)

try:
    status, _, _ = http("GET", "/actuator/health")
    check("Gateway health", status == 200, status)
    assert status == 200

    nonce = uuid.uuid4().hex
    users, tokens, accounts = [], [], []
    credentials = []
    for index in range(2):
        email = f"transfer-e2e-{nonce}-{index}@example.invalid"
        password = "E2eTransfer" + uuid.uuid4().hex[:12]
        credentials.append({"email": email, "password": password})
        status, body, _ = http("POST", "/api/v1/auth/register", {
            "email": email, "password": password, "name": "E2E Transfer", "phoneNumber": "00000000000"})
        check(f"User {index + 1} registered through Gateway", status == 201, status)
        assert status == 201
        users.append(str(uuid.UUID(body["userId"])))
        status, body, _ = http("POST", "/api/v1/auth/login", {"email": email, "password": password})
        check(f"User {index + 1} logged in through Gateway", status == 200, status)
        assert status == 200
        tokens.append(body["accessToken"])
        status, body, _ = http("POST", "/api/v1/accounts", {"currency": "KRW", "alias": "E2E " + str(index + 1)}, tokens[-1])
        check(f"Account {index + 1} created with verified JWT", status == 201, status)
        assert status == 201
        accounts.append(str(uuid.UUID(body["accountId"])))

    check("No account proxy workaround is configured", not CONTEXT["account_proxy_workaround"])
    check("Newly opened accounts have zero balance", Decimal(balance(accounts[0], tokens[0])) == 0 and Decimal(balance(accounts[1], tokens[1])) == 0)
    status, profile, _ = http("GET", "/api/v1/users/me", token=tokens[0])
    check("Auth user profile receives verified Gateway identity", status == 200 and profile.get("userId") == users[0])
    reject("Another user's balance is forbidden", "GET", f"/api/v1/accounts/{accounts[0]}/balances", 403, token=tokens[1])
    reject("Another user's entries are forbidden", "GET", f"/api/v1/accounts/{accounts[0]}/entries", 403, token=tokens[1])
    reject("Spoofed user identity is scrubbed", "GET", f"/api/v1/accounts/{accounts[0]}/balances", 403, token=tokens[1], headers={"X-User-Id": users[0]})
    reject("Unknown account remains 404", "GET", f"/api/v1/accounts/{uuid.uuid4()}/balances", 404, token=tokens[0])
    initial_body = {"fromAccountId": accounts[0], "toAccountId": accounts[1], "amount": 10000, "currency": "KRW"}
    reject("Missing OTP is rejected before reservation", "POST", "/api/v1/transfers", 401, initial_body, tokens[0], {"Idempotency-Key": "no-otp-"+nonce})
    reject("Forged OTP is rejected", "POST", "/api/v1/transfers", 401, {**initial_body,"otpToken":"forged"}, tokens[0], {"Idempotency-Key":"bad-otp-"+nonce})
    bad_signature = tokens[0][:-10] + ("A" if tokens[0][-10] != "A" else "B") + tokens[0][-9:]
    reject("Invalid JWT signature is rejected", "GET", "/api/v1/accounts", 401, token=bad_signature)
    reject("Gateway denies internal endpoints", "GET", f"/internal/v1/accounts/{accounts[0]}", 403, token=tokens[0])

    REPORT["accounts"] = {"source": accounts[0], "destination": accounts[1]}
    # Opening funds are fixtures, not the transaction being verified. All subsequent
    # debit, credit, journal, posting, history and event rows must come from the APIs.
    maria("ledger_db", "INSERT INTO ledger_accounts(id,external_account_id,balance,currency,version) VALUES "
          + f"('{uuid.uuid4()}','{accounts[0]}',100000,'KRW',0),"
          + f"('{uuid.uuid4()}','{accounts[1]}',0,'KRW',0);")
    before = {"source": balance(accounts[0], tokens[0]), "destination": balance(accounts[1], tokens[1])}
    REPORT["balances_before"] = before
    check("Initial balance fixture visible through Gateway -> Ledger", Decimal(before["source"]) == 100000 and Decimal(before["destination"]) == 0, before)

    idempotency_key = "e2e-" + nonce
    trace_id = uuid.uuid4().hex
    REPORT["idempotency_key"] = idempotency_key
    proof = issue_proof(0)
    status, body, response_trace = http("POST", "/api/v1/transfers", {
        "fromAccountId": accounts[0], "toAccountId": accounts[1], "amount": 10000,
        "currency": "KRW", "otpToken": proof
    }, tokens[0], {"Idempotency-Key": idempotency_key, "X-Trace-Id": trace_id})
    REPORT["transfer_api"] = {"status": status, "response": body, "response_trace_id": response_trace}
    check("Single transfer request returns HTTP 200 COMPLETED", status == 200 and body.get("status") == "COMPLETED", REPORT["transfer_api"])

    transfers = pg("transfer_db", "SELECT id,transaction_id,status,amount::text,currency,from_account_id,to_account_id FROM transfers WHERE idempotency_key='" + idempotency_key + "'")
    assert len(transfers) == 1, "Transfer outcome was not durably identifiable; do not submit another financial request"
    transfer = transfers[0]
    transfer_id, tx_id = str(uuid.UUID(transfer["id"])), str(uuid.UUID(transfer["transaction_id"]))
    REPORT["transfer_database"] = transfer
    check("Exactly one durable completed transfer", transfer["status"] == "COMPLETED" and Decimal(transfer["amount"]) == 10000, transfer)

    after = {"source": balance(accounts[0], tokens[0]), "destination": balance(accounts[1], tokens[1])}
    REPORT["balances_after"] = after
    check("Source debited once and destination credited once", Decimal(after["source"]) == 90000 and Decimal(after["destination"]) == 10000, after)
    check("Total money conserved", sum(map(Decimal, before.values())) == sum(map(Decimal, after.values())))

    journals = maria_rows("ledger_db", "SELECT JSON_OBJECT('journalId',id,'transactionId',transaction_id) FROM journals;")
    postings = maria_rows("ledger_db", "SELECT JSON_OBJECT('journalId',journal_id,'ledgerAccountId',ledger_account_id,'amount',CAST(signed_amount AS CHAR),'currency',currency) FROM postings;")
    REPORT["journals"], REPORT["postings"] = journals, postings
    check("One journal for the same transactionId", len(journals) == 1 and journals[0]["transactionId"] == tx_id, journals)
    check("Two balanced postings, -10000 and +10000 KRW", len(postings) == 2 and sorted(Decimal(p["amount"]) for p in postings) == [Decimal(-10000), Decimal(10000)] and sum(Decimal(p["amount"]) for p in postings) == 0 and all(p["currency"] == "KRW" for p in postings), postings)

    query_status, query_body, _ = http("GET", "/api/v1/transfers/" + transfer_id, token=tokens[0])
    check("Transfer status API returns the committed result", query_status == 200 and query_body.get("status") == "COMPLETED" and query_body.get("transactionId") == tx_id)
    for index in range(2):
        entry_status, entry_body, _ = http("GET", f"/api/v1/accounts/{accounts[index]}/entries", token=tokens[index])
        entries = entry_body.get("entries", [])
        check(f"Account {index + 1} transaction history routes to Ledger", entry_status == 200 and len(entries) == 1 and entries[0].get("transactionId") == tx_id)

    duplicate = reject("Same request is 409 without another debit", "POST", "/api/v1/transfers", 409,
        {**initial_body,"otpToken":proof},tokens[0],{"Idempotency-Key":idempotency_key})
    check("Duplicate response identifies the original transfer", duplicate.get("existingTransferId") == transfer_id)
    reject("Same OTP cannot authorize another transfer", "POST", "/api/v1/transfers", 401,
        {**initial_body,"otpToken":proof},tokens[0],{"Idempotency-Key":"replay-"+nonce})
    reject("Another user cannot use an OTP authorization", "POST", "/api/v1/transfers", 401,
        {"fromAccountId":accounts[1],"toAccountId":accounts[0],"amount":1,"currency":"KRW","otpToken":proof},tokens[1],{"Idempotency-Key":"other-proof-"+nonce})
    other_proof = issue_proof(1)
    reject("Another user cannot debit the source account", "POST", "/api/v1/transfers", 403,
        {**initial_body,"otpToken":other_proof},tokens[1],{"Idempotency-Key":"other-source-"+nonce})
    insufficient_proof = issue_proof(0)
    reject("Insufficient funds is 422 rather than 500", "POST", "/api/v1/transfers", 422,
        {**initial_body,"amount":200000,"otpToken":insufficient_proof},tokens[0],{"Idempotency-Key":"insufficient-"+nonce})

    account_base = "http://" + compose("port", "dev-access", "8082").strip()
    internal_headers = {"X-Internal-Caller":"e2e-ops","X-Internal-Service-Token":INTERNAL_TOKEN}
    reject("Internal account lookup requires service authentication", "GET", f"/internal/v1/accounts/{accounts[0]}", 403, base=account_base)
    reject("Encoded internal path cannot bypass authentication", "GET", f"/%69nternal/v1/accounts/{accounts[0]}", 403, base=account_base)
    reject("Encoded status PATCH cannot bypass authentication", "PATCH", f"/%61pi/v1/accounts/{accounts[0]}/status", 403, {"status":"SUSPENDED","reason":"E2E"}, base=account_base)
    status, _, _ = http("PATCH", f"/api/v1/accounts/{accounts[0]}/status", {"status":"SUSPENDED","reason":"E2E"}, headers=internal_headers, base=account_base)
    check("Authenticated internal status change succeeds", status == 200, status)
    suspended_proof = issue_proof(0)
    reject("Suspended account cannot transfer", "POST", "/api/v1/transfers", 409,
        {**initial_body,"amount":1,"otpToken":suspended_proof},tokens[0],{"Idempotency-Key":"suspended-"+nonce})
    status, _, _ = http("PATCH", f"/api/v1/accounts/{accounts[0]}/status", {"status":"ACTIVE","reason":"E2E"}, headers=internal_headers, base=account_base)
    check("Account status can be restored", status == 200, status)

    expired = issue_proof(0)
    encoded = expired.split(".")[1]
    proof_id = str(uuid.UUID(base64.urlsafe_b64decode(encoded + "=" * (-len(encoded) % 4)).decode().split(".")[0]))
    execute("postgres", ["sh","-c",'exec psql -X -U "$POSTGRES_USER" -d auth_db -v ON_ERROR_STOP=1'],
        "update otp_proofs set expires_at=now()-interval '1 second' where id='"+proof_id+"';")
    reject("Expired authorization cannot transfer", "POST", "/api/v1/transfers", 401,
        {**initial_body,"amount":1,"otpToken":expired},tokens[0],{"Idempotency-Key":"expired-"+nonce})

    draft = str(uuid.uuid4())
    for i in range(5):
        reject("Wrong OTP code is rejected", "POST", "/api/v1/auth/otp/verify", 401,
            {"transferDraftId":draft,"otpCode":"invalid"},tokens[0])
    reject("Repeated OTP failures lock the draft with 423", "POST", "/api/v1/auth/otp/verify", 423,
        {"transferDraftId":draft,"otpCode":"invalid"},tokens[0])

    reject("Logout completes", "POST", "/api/v1/auth/logout", 204, {}, tokens[0])
    reject("Logged-out JWT cannot access protected APIs", "GET", "/api/v1/accounts", 401, token=tokens[0])
    login_status, login_body, _ = http("POST", "/api/v1/auth/login", credentials[0])
    check("Fresh login remains possible after logout", login_status == 200)
    tokens[0] = login_body["accessToken"]
    final_balances = {"source":balance(accounts[0],tokens[0]),"destination":balance(accounts[1],tokens[1])}
    check("Rejected requests never changed either balance", final_balances == after, final_balances)
    request_states = pg("transfer_db", "select status,count(*) as count from transfers group by status order by status")
    check("Only success and insufficient-funds requests were reserved", {x["status"]:x["count"] for x in request_states} == {"COMPLETED":1,"FAILED":1}, request_states)

    deadline = time.monotonic() + 45
    while True:
        ledger_events, transfer_events = ledger_outbox(), transfer_outbox()
        if len(ledger_events) == len(transfer_events) == 1 and all(e.get("publishedAt") for e in ledger_events + transfer_events):
            break
        if time.monotonic() >= deadline:
            break
        time.sleep(1)
    REPORT["outbox"] = {"ledger": ledger_events, "transfer": transfer_events}
    check("Both local Outbox rows were ACKed by Kafka", len(ledger_events) == len(transfer_events) == 1 and all(e.get("publishedAt") for e in ledger_events + transfer_events), REPORT["outbox"])

    kafka_results = {}
    for topic, expected_events in [("ledger.posted", ledger_events), ("transfer.completed", transfer_events)]:
        output = kafka("/opt/kafka/bin/kafka-console-consumer.sh", "--bootstrap-server", "kafka:19092", "--topic", topic, "--from-beginning", "--max-messages", "1", "--timeout-ms", "20000", "--group", "e2e-observer-" + nonce + "-" + topic)
        messages = [json.loads(line) for line in output.splitlines() if line.lstrip().startswith("{")]
        check(topic + " is readable from the real Kafka broker", len(messages) == 1 and messages[0].get("transactionId") == tx_id and messages[0].get("journalId") == journals[0]["journalId"] and messages[0].get("eventId") == expected_events[0]["eventId"])
        offsets = kafka("/opt/kafka/bin/kafka-get-offsets.sh", "--bootstrap-server", "kafka:19092", "--topic", topic, "--time", "-1").splitlines()
        record_count = sum(int(line.rsplit(":", 1)[1]) for line in offsets if line.startswith(topic + ":"))
        check(topic + " contains one record in this clean broker", record_count == 1, record_count)
        kafka_results[topic] = {"messages": messages, "partition_end_offsets": offsets, "record_count": record_count}
    REPORT["kafka"] = kafka_results

    description = kafka("/opt/kafka/bin/kafka-consumer-groups.sh", "--bootstrap-server", "kafka:19092", "--describe", "--group", "transfer")
    group_rows = [line.split() for line in description.splitlines() if line.split()[:2] == ["transfer", "ledger.posted"]]
    processed = [row for row in group_rows if len(row) >= 6 and row[4].isdigit() and int(row[4]) > 0]
    check("Transfer consumer committed ledger.posted with zero lag", bool(processed) and all(row[3] == row[4] and row[5] == "0" for row in processed))
    REPORT["transfer_consumer_group"] = description.splitlines()
    histories = pg("transfer_db", "SELECT previous_status,next_status,reason_code FROM transfer_histories WHERE transfer_id='" + transfer_id + "' ORDER BY occurred_at")
    REPORT["state_history"] = histories
    check("REST and event completion leave one completion transition", sum(h["next_status"] == "COMPLETED" for h in histories) == 1, histories)
    check("Only one transfer journal exists after all rejection probes", len(maria_rows("ledger_db", "SELECT JSON_OBJECT('id',id) FROM journals;")) == 1)
    # MVP2-F12 regression: the real PostgreSQL/MariaDB-backed apps must export the
    # exact names already used by the alert rules. Scrapes are local to this run.
    for service, port in [("ledger", "8083"), ("transfer", "8084")]:
        metrics_url = "http://" + compose("port", "dev-access", port).strip() + "/actuator/prometheus"
        values = {}
        deadline = time.monotonic() + 35
        while time.monotonic() < deadline:
            with urllib.request.urlopen(metrics_url, timeout=10) as response:
                lines = response.read().decode().splitlines()
            for line in lines:
                if line.startswith("pg_outbox_"):
                    name, value = line.rsplit(" ", 1)
                    values[name.split("{", 1)[0]] = float(value)
            if values.get("pg_outbox_unpublished_events") == 0 and values.get("pg_outbox_oldest_unpublished_age_seconds") == 0:
                break
            time.sleep(1)
        check(service + " exports committed unpublished count", values.get("pg_outbox_unpublished_events") == 0)
        check(service + " exports oldest unpublished age", values.get("pg_outbox_oldest_unpublished_age_seconds") == 0)

    REPORT["passed"] = all(item["passed"] for item in REPORT["checks"])
    REPORT["finished_at_kst"] = datetime.now(ZoneInfo("Asia/Seoul")).isoformat()
    save()
    print("E2E verdict: " + ("PASS" if REPORT["passed"] else "FAIL"), flush=True)
    raise SystemExit(0 if REPORT["passed"] else 1)
except Exception as error:
    REPORT["passed"] = False
    REPORT["failure"] = {"type": type(error).__name__, "message": str(error)}
    save()
    raise
