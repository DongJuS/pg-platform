#!/usr/bin/env python3
"""Create local-only credentials without printing them or overwriting an existing file."""
import argparse
import os
from pathlib import Path
import secrets
import tempfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("path", type=Path, help="Absolute env-file path outside the repository")
parser.add_argument("--upgrade", action="store_true", help="Add missing secrets without replacing existing values")
args = parser.parse_args()
destination = args.path.expanduser()
repository = Path(__file__).resolve().parents[2]
if not destination.is_absolute() or destination.resolve().is_relative_to(repository):
    parser.error("Choose an absolute path outside the repository.")
values = {name: secrets.token_hex(24) for name in (
    "POSTGRES_PASSWORD", "MARIADB_ROOT_PASSWORD", "APP_DB_PASSWORD", "GRAFANA_ADMIN_PASSWORD"
)}
values.update(AI_PREPARATION_WEALTH_TOKEN=secrets.token_hex(32), INTERNAL_SERVICE_TOKEN=secrets.token_hex(32), AUTH_OTP_SECRET_HEX=secrets.token_hex(32),
              PG_WEBHOOK_SECRET_HEX=secrets.token_hex(32), MOCK_PG_TOKEN=secrets.token_hex(32))
if args.upgrade:
    if not destination.is_file():
        parser.error("Upgrade requires an existing env file.")
    original = destination.read_text()
    lines = original.splitlines()
    existing = {}
    for index, line in enumerate(lines):
        if "=" in line and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            existing[key.strip()] = (index, value)
    added = []
    for key, value in values.items():
        if key in existing and existing[key][1].strip().strip("\"'"):
            continue
        if key in existing:
            lines[existing[key][0]] = key + "=" + value
        else:
            lines.append(key + "=" + value)
        added.append(key)
    with tempfile.NamedTemporaryFile(mode="w", dir=destination.parent, delete=False) as output:
        output.write("\n".join(lines) + "\n")
        temporary = output.name
    os.chmod(temporary, 0o600)
    os.replace(temporary, destination)
    print("Added missing secret names: " + (", ".join(added) or "none"))
    raise SystemExit(0)
try:
    descriptor = os.open(destination, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
except FileExistsError:
    parser.error("File already exists; existing credentials were preserved.")
with os.fdopen(descriptor, "w") as output:
    output.write("# Local development only. Keep this file outside version control.\n")
    for name, value in values.items():
        output.write(f"{name}={value}\n")
print(f"Created private env file: {destination}")
