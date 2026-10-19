#!/usr/bin/env python3
"""Check Docker app readiness without creating financial transactions or exposing secrets."""
import argparse
import json
from pathlib import Path
import subprocess
import sys
import urllib.error
import urllib.request

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("env_file", type=Path)
parser.add_argument("services", nargs="*", default=["api-gateway", "auth", "account", "ledger", "transfer"])
parser.add_argument("--project-name", default="sinhan_project_docker")
args = parser.parse_args()
directory = Path(__file__).resolve().parent
command = ["sh", str(directory / "stack.sh"), str(args.env_file.resolve()),
           "--project-name", args.project_name, "--profile", "apps"]
# Resolved config contains secrets: capture it and never print it, including on error.
result = subprocess.run(command + ["config", "--format", "json"], capture_output=True, text=True)
if result.returncode:
    sys.exit("Compose configuration could not be resolved. Check the env file and Compose version.")
configuration = json.loads(result.stdout)
failures = []
for name in args.services:
    service = configuration["services"].get(name)
    if service is None or "build" not in service:
        failures.append(f"{name}: unknown application service")
        continue
    mapping = next((port for port in configuration["services"]["dev-access"].get("ports", [])
                    if port.get("host_ip") == "127.0.0.1"
                    and str(port["target"]) == service["environment"]["SERVER_PORT"]), None)
    if mapping is None:
        failures.append(f"{name}: no loopback HTTP port")
        continue
    address = f"http://127.0.0.1:{mapping['published']}/actuator/health"
    try:
        with urllib.request.urlopen(address, timeout=10) as response:
            if response.status != 200 or json.load(response).get("status") != "UP":
                raise ValueError("health is not UP")
        print(f"PASS {name}: health UP")
    except (urllib.error.URLError, ValueError, TimeoutError):
        failures.append(f"{name}: not ready")
if failures:
    sys.exit("\n".join(failures))
print("Readiness only: business flows, AWS IAM/networking and load are not verified by this check.")
