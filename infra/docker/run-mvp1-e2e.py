#!/usr/bin/env python3
"""Build and verify MVP1 in a disposable Docker project; never reuse application data."""
import argparse
import json
import hashlib
import os
from pathlib import Path
import secrets
import signal
import subprocess
import sys
import tempfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--output", type=Path, help="Credential-free result JSON destination")
parser.add_argument("--suite", choices=["MVP1", "MVP2", "MVP3", "MVP4", "MVP5"], default="MVP1")
parser.add_argument("--observability", action="store_true", help="Include isolated metrics, logs, traces and local alerts")
parser.add_argument("--swift-only", action="store_true", help="Debug only MVP5 business probes; final acceptance must omit this flag")
args = parser.parse_args()
if args.swift_only and args.suite != "MVP5": parser.error("--swift-only requires --suite MVP5")
repo = Path(__file__).resolve().parents[2]
run = Path(tempfile.mkdtemp(prefix="pg-" + args.suite.lower() + "-e2e-", dir="/private/tmp" if Path("/private/tmp").exists() else None))
project = "pg_" + args.suite.lower() + "_" + secrets.token_hex(4)
tag = args.suite.lower() + "-" + secrets.token_hex(4)
configuration = run / "compose.json"
env_file = run / "local.env"
target_services = ["api-gateway", "auth", "account", "ledger", "transfer"]
if args.suite in {"MVP2", "MVP3", "MVP4", "MVP5"}:
    target_services += ["payment", "fep-adapter", "mock-pg"]
if args.suite in {"MVP3", "MVP4", "MVP5"}:
    target_services += ["ai-gateway"]
if args.suite in {"MVP4", "MVP5"}:
    target_services += ["wealth-report"]
if args.suite == "MVP5":
    target_services += ["swift", "mock-swift"]
context = {"directory": str(run), "project": project, "image_registry": "sinhan_mvp1_test",
           "image_tag": tag, "services": target_services, "account_proxy_workaround": False,
           "otp_verified": True, "regressions_skipped_for_debug": args.swift_only, "source_working_tree_has_changes": True, "observability": args.observability}

def docker(*arguments, log=None, timeout=120):
    command = ["docker", *arguments]
    if log:
        with (run / log).open("w") as output:
            subprocess.run(command, cwd=repo, stdout=output, stderr=subprocess.STDOUT, check=True, timeout=timeout)
        return ""
    return subprocess.check_output(command, cwd=repo, text=True, timeout=timeout)

def interrupted(signum, frame):
    raise RuntimeError("Runner interrupted; cleaning its isolated Docker project")

signal.signal(signal.SIGTERM, interrupted)

def original_containers():
    return sorted(docker("ps", "--filter", "label=com.docker.compose.project=sinhan_project_docker", "--format", "{{.ID}} {{.Names}}").splitlines())

fingerprint = hashlib.sha256()
for path in sorted(repo.rglob("*")):
    if not path.is_file() or any(part in {".git", ".gradle", "build", ".terraform", "__pycache__"} for part in path.relative_to(repo).parts):
        continue
    relative = path.relative_to(repo)
    if "src" in relative.parts or (len(relative.parts) == 1 and path.suffix in {".kts", ".properties"}) or relative.parts[0] == "gradle":
        fingerprint.update(str(relative).encode() + b"\0" + hashlib.sha256(path.read_bytes()).digest())
context["source_sha256"] = fingerprint.hexdigest()
result = {"passed": False, "context": context}
configured = False
try:
    subprocess.run([sys.executable, str(repo / "infra/docker/init-env.py"), str(env_file)], check=True, timeout=30)
    with env_file.open("a") as output:
        output.write("IMAGE_REGISTRY=sinhan_mvp1_test\nIMAGE_TAG=" + tag + "\n")
    compose_inputs = ["compose", "--env-file", str(env_file), "--project-name", project,
                      "-f", str(repo / "infra/docker/compose.yaml"),
                      "-f", str(repo / "infra/docker/compose.outbox.yaml")]
    if args.observability:
        compose_inputs += ["-f", str(repo / "infra/docker/compose.observability.yaml")]
    raw = docker(*compose_inputs, "--profile", "apps", "--profile", "observability", "config", "--format", "json")
    config = json.loads(raw)
    keep = set(target_services) | {"postgres", "mariadb", "redis", "kafka", "kafka-init", "kafka-volume-init", "dev-access", "otel-collector"}
    if args.observability:
        keep |= {"prometheus", "grafana", "alertmanager", "tempo", "loki", "alloy"}
    config["services"] = {name: value for name, value in config["services"].items() if name in keep}
    for value in config["services"].values():
        value.pop("profiles", None)
    assert not config["services"]["account"]["environment"].get("SPRING_AOP_PROXY_TARGET_CLASS"), "Do not use the account proxy workaround"
    assert config["services"]["transfer"]["environment"]["OUTBOX_ENABLED"] == "true"
    assert config["services"]["ledger"]["environment"]["OUTBOX_ENABLED"] == "true"
    for value in list(config["networks"].values()) + list(config["volumes"].values()):
        assert value["name"].startswith(project + "_"), "Refusing shared resources"
    config["services"]["dev-access"]["ports"] = [
        {"target": port, "published": "0", "host_ip": "127.0.0.1", "protocol": "tcp"}
        for port in ([8080, 8082, 8083, 8084, 8085, 8091, 8092] if args.suite in {"MVP2", "MVP3", "MVP4", "MVP5"} else [8080, 8082, 8083, 8084])
    ]
    if args.suite in {"MVP3", "MVP4", "MVP5"}:
        config["services"]["dev-access"]["ports"].append(
            {"target": 8090, "published": "0", "host_ip": "127.0.0.1", "protocol": "tcp"})
        # Test before the external boundary with no internet network attached to AI Gateway.
        config["services"]["ai-gateway"]["networks"].pop("ai-egress", None)
        config["services"]["ai-gateway"]["environment"]["PG_INTERNAL_CALLER_TOKENS_PAYMENT"] = "synthetic-denied-caller-credential"
        context["ai_gateway_internet_egress"] = False
    if args.suite in {"MVP4", "MVP5"}:
        config["services"]["wealth-report"]["environment"].update({
            "SPRING_PROFILES_ACTIVE": "docker,skeleton,report-test",
            "PLATFORM_REPORT_POLICY_CONFIRMED": "true",
            "PLATFORM_REPORT_EVENTS_ENABLED": "true"})
        config["services"]["dev-access"]["ports"].append(
            {"target": 8088, "published": "0", "host_ip": "127.0.0.1", "protocol": "tcp"})
        context["report_explanation_source"] = "TEST_DOUBLE"
        context["report_policy"] = "proposed-policy-enabled-for-isolated-test-only"
    if args.suite == "MVP5":
        credentials = {name: secrets.token_urlsafe(32) for name in ["transfer", "swift", "swift-operations", "mock-swift"]}
        payload_key = __import__("base64").b64encode(secrets.token_bytes(32)).decode()
        mock_token = secrets.token_urlsafe(32)
        context["swift_institution"] = "LOCAL_DURABLE_MOCK_ONLY"
        context["swift_policy"] = "USD-no-FX-zero-fee-48h-private-data"
        # The test administrator UUID is trusted configuration, not a user-writable role header.
        context["swift_admin_user_id"] = "50000000-0000-0000-0000-000000000001"
        for service in ["account", "transfer", "ledger", "swift", "fep-adapter", "mock-swift"]:
            env = config["services"][service]["environment"]
            env.update({"SPRING_PROFILES_ACTIVE":"docker,skeleton,swift-test", "PG_SWIFT_ENABLED":"true",
                        "PG_SWIFT_PAYLOAD_KEY":payload_key,"PG_SWIFT_MOCK_TOKEN":mock_token,
                        "PG_SWIFT_RETRANSMIT_ENABLED":"true","PG_SWIFT_MINIMUM_RETRANSMIT_SECONDS":"2",
                        "PG_SWIFT_RECOVERY_INTERVAL_MS":"1000","PG_SWIFT_ADMIN_USER_IDS":context["swift_admin_user_id"]})
            for caller, credential in credentials.items():
                env["PG_SWIFT_CALLER_TOKENS_" + caller.replace("-", "_").upper()] = credential
        for service in ["account", "transfer", "ledger", "swift", "fep-adapter", "mock-swift"]:
            private_logs = run / ("private-logs-" + service)
            private_logs.mkdir(mode=0o777)
            private_logs.chmod(0o777)
            config["services"][service]["volumes"].append({"type":"bind","source":str(private_logs),"target":"/private-logs"})
            config["services"][service]["environment"]["SWIFT_PRIVATE_LOG_DIR"] = "/private-logs/data"
        config["services"]["ledger-loss-proxy"] = {
            "image":"python:3.13-alpine", "user":"10001:10001", "entrypoint":["python","/proxy.py"], "read_only":True,
            "cap_drop":["ALL"],"security_opt":["no-new-privileges:true"],"mem_limit":"64m",
            "environment":{"PYTHONDONTWRITEBYTECODE":"1"},"networks":{"main":None},
            "volumes":[{"type":"bind","source":str(repo / "infra/docker/swift-ledger-loss-proxy.py"),"target":"/proxy.py","read_only":True}],
            "depends_on":{"ledger":{"condition":"service_healthy"}},
            "healthcheck":{"test":["CMD","python","-c","import urllib.request; urllib.request.urlopen('http://127.0.0.1:18083/health',timeout=2)"],"interval":"2s","timeout":"3s","retries":20}}
        config["services"]["transfer"]["depends_on"]["ledger-loss-proxy"]={"condition":"service_healthy"}
        config["services"]["transfer"]["environment"]["PG_CLIENTS_LEDGER_BASE_URL"]="http://ledger-loss-proxy:18083"
        config["services"]["swift"]["environment"]["OUTBOX_ENABLED"] = "true"
        for port in [8087,8093]:
            config["services"]["dev-access"]["ports"].append({"target":port,"published":"0","host_ip":"127.0.0.1","protocol":"tcp"})
        # A brand-new, test-owned bind directory survives Mock process restarts and is removed after cleanup.
        mock_state = run / "mock-state"
        mock_state.mkdir(mode=0o777)
        mock_state.chmod(0o777)
        config["services"]["mock-swift"]["volumes"].append({"type":"bind","source":str(mock_state),"target":"/state"})
        config["services"]["mock-swift"]["environment"]["PG_SWIFT_MOCK_DIRECTORY"] = "/state/data"
        with (run / "swift-test-secrets.json").open("w") as private:
            __import__("os").chmod(run / "swift-test-secrets.json",0o600)
            json.dump({"callers":credentials,"mock_token":mock_token},private)
    if args.observability:
        alloy = run / "alloy.alloy"
        alloy.write_text((repo / "infra/observability/alloy.alloy").read_text().replace("com.docker.compose.project=sinhan_project_docker", "com.docker.compose.project=" + project))
        for mount in config["services"]["alloy"]["volumes"]:
            if mount.get("target") == "/etc/alloy/config.alloy":
                mount["source"] = str(alloy)
        for service in target_services:
            config["services"][service]["environment"]["TRACE_SAMPLING_PROBABILITY"] = "1.0"
        for service, port in {"prometheus":9090,"grafana":3000,"alertmanager":9093,"tempo":3200,"loki":3100}.items():
            config["services"][service]["ports"] = [{"target":port,"published":"0","host_ip":"127.0.0.1","protocol":"tcp"}]
            networks = config["services"][service]["networks"]
            if isinstance(networks, dict):
                networks["obs-ingress"] = None
            elif "obs-ingress" not in networks:
                networks.append("obs-ingress")
    descriptor = os.open(configuration, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(descriptor, "w") as output:
        json.dump(config, output)
    configured = True
    before = original_containers()
    (run / "context.json").write_text(json.dumps(context, indent=2))
    docker("compose", "-f", str(configuration), "config", "--quiet")
    print("Building isolated MVP1 images...", flush=True)
    docker("compose", "-f", str(configuration), "--parallel", "1", "--progress", "plain", "build", *target_services, log="build.log", timeout=1200)
    print("Starting isolated MVP1 stack without proxy workaround...", flush=True)
    docker("compose", "-f", str(configuration), "up", "-d", "--wait", "--wait-timeout", "240", log="start.log", timeout=360)
    print("Running authenticated transfer and rejection checks...", flush=True)
    if args.swift_only:
        completed = subprocess.CompletedProcess([], 0)
        result = {"passed":True,"checks":[],"context":context}
    else:
        completed = subprocess.run([sys.executable, str(repo / "infra/docker/verify-mvp1.py"), str(run)], cwd=repo, timeout=300)
    if (run / "result.json").exists():
        result = json.loads((run / "result.json").read_text())
    if not args.swift_only and args.suite in {"MVP2", "MVP3", "MVP4", "MVP5"} and completed.returncode == 0:
        extra = subprocess.run([sys.executable, str(repo / "infra/docker/verify-mvp2.py"), str(run)], cwd=repo, timeout=360)
        if (run / "mvp2-result.json").exists():
            payment_result = json.loads((run / "mvp2-result.json").read_text())
            result["mvp2"] = payment_result
            result["checks"].extend(payment_result["checks"])
            result["passed"] = result["passed"] and payment_result["passed"] and extra.returncode == 0
        else:
            result["passed"] = False
    if not args.swift_only and args.suite in {"MVP3", "MVP4", "MVP5"} and result.get("passed"):
        extra = subprocess.run([sys.executable, str(repo / "infra/docker/verify-mvp3.py"), str(run)], cwd=repo, timeout=300)
        if (run / "mvp3-result.json").exists():
            ai_result = json.loads((run / "mvp3-result.json").read_text())
            result["mvp3"] = ai_result
            result["checks"].extend(ai_result["checks"])
            result["passed"] = result["passed"] and ai_result["passed"] and extra.returncode == 0
        else:
            result["passed"] = False
    if not args.swift_only and args.suite in {"MVP4", "MVP5"} and result.get("passed"):
        extra = subprocess.run([sys.executable, str(repo / "infra/docker/verify-mvp4.py"), str(run)], cwd=repo, timeout=300)
        if (run / "mvp4-result.json").exists():
            report_result = json.loads((run / "mvp4-result.json").read_text())
            result["mvp4"] = report_result
            result["checks"].extend(report_result["checks"])
            result["passed"] = result["passed"] and report_result["passed"] and extra.returncode == 0
        else:
            result["passed"] = False
    if args.suite == "MVP5" and result.get("passed"):
        extra = subprocess.run([sys.executable, str(repo / "infra/docker/verify-mvp5.py"), str(run)], cwd=repo, timeout=480)
        if (run / "mvp5-result.json").exists():
            swift_result = json.loads((run / "mvp5-result.json").read_text())
            result["mvp5"] = swift_result
            result["checks"].extend(swift_result["checks"])
            result["passed"] = result["passed"] and swift_result["passed"] and extra.returncode == 0
        else:
            result["passed"] = False
    result["existing_stack_preserved"] = before == original_containers()
    result["images"] = {}
    for service in target_services:
        name = "sinhan_mvp1_test/" + service + ":" + tag
        data = json.loads(docker("image", "inspect", name))[0]
        result["images"][service] = {"image": name, "id": data["Id"], "architecture": data["Architecture"]}
    result["passed"] = bool(result.get("passed")) and completed.returncode == 0 and result["existing_stack_preserved"]
except Exception as error:
    result["passed"] = False
    result["runner_failure"] = {"type": type(error).__name__, "message": "See private runner logs in " + str(run)}
finally:
    result["cleanup_complete"] = not configured
    if configured:
        try:
            docker("compose", "-f", str(configuration), "logs", "--no-color", log="service-logs.txt")
        except Exception:
            pass
        print("Cleaning isolated MVP1 stack...", flush=True)
        try:
            docker("compose", "-f", str(configuration), "down", "--volumes", log="cleanup.log")
            result["cleanup_complete"] = True
        except Exception:
            result["cleanup_complete"] = False
            result["passed"] = False
    if result.get("cleanup_complete") and args.suite == "MVP5":
        import shutil
        for private_directory in [run / "mock-state", *run.glob("private-logs-*")]:
            if private_directory.exists():
                shutil.rmtree(private_directory)
        result["private_test_artifacts_removed"] = True
    for secret_file in ([env_file, configuration, run / "swift-test-secrets.json"] if result.get("cleanup_complete") else []):
        if secret_file.exists():
            secret_file.unlink()
    (run / "result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2))
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    print("Result: " + str(args.output or run / "result.json"), flush=True)
raise SystemExit(0 if result["passed"] else 1)
