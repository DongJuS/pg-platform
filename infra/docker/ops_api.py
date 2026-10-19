#!/usr/bin/env python3
"""Local-only asynchronous MVP1 acceptance API. No Docker socket in banking services."""
import argparse
from datetime import datetime, timezone
import fcntl
import hashlib
import hmac
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
from pathlib import Path
import re
import secrets
import subprocess
import sys
import threading
from urllib.parse import urlsplit
import uuid

COLLECTION = "/ops/v1/performance-runs"
ACTIVE = "RUNNING"


def now():
    return datetime.now(timezone.utc).isoformat()


def save(path, value):
    temporary = path.with_suffix(".tmp")
    with temporary.open("w", encoding="utf-8") as output:
        json.dump(value, output, ensure_ascii=False, indent=2)
        output.write("\n")
    os.chmod(temporary, 0o600)
    temporary.replace(path)


class ApiError(Exception):
    def __init__(self, status, code, message, **details):
        super().__init__(message)
        self.status = status
        self.body = {"code": code, "message": message, **details}


class DockerMvp1Runner:
    """Only the fixed repository-owned suite can execute, without a shell."""
    def __init__(self, repo, lock_fd):
        self.repo, self.lock_fd = repo, lock_fd

    def __call__(self, directory, progress):
        output = directory / "evidence.json"
        phases = {"Building isolated": "BUILDING", "Starting isolated": "STARTING",
                  "Running authenticated": "VERIFYING", "Cleaning isolated": "CLEANING"}
        with (directory / "runner.log").open("w") as log:
            # Inherit the exclusive lock: after an abrupt API exit another server
            # must wait until this child has finished (including Docker cleanup).
            with subprocess.Popen([sys.executable, str(self.repo / "infra/docker/run-mvp1-e2e.py"),
                                   "--output", str(output)], cwd=self.repo,
                                  stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                  text=True, pass_fds=(self.lock_fd,)) as process:
                for line in process.stdout:
                    log.write(line)
                    log.flush()
                    for prefix, phase in phases.items():
                        if line.startswith(prefix):
                            progress(phase)
                code = process.wait()
        result = json.loads(output.read_text()) if output.exists() else {}
        if code != 0:
            result["passed"] = False
        return result


class RunStore:
    """One active disposable stack, durable result history, no arbitrary run parameters."""
    def __init__(self, repo, directory, runner=None):
        self.directory = directory
        directory.mkdir(mode=0o700, parents=True, exist_ok=True)
        os.chmod(directory, 0o700)
        self.process_lock = (directory / "server.lock").open("a+")
        try:
            fcntl.flock(self.process_lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            self.process_lock.close()
            raise RuntimeError("An operations server or its Docker run is already active in this state directory")
        self.lock = threading.RLock()
        self.runs = {}
        self.worker = None
        self.runner = runner or DockerMvp1Runner(repo, self.process_lock.fileno())
        for path in sorted(directory.glob("*/run.json")):
            run = json.loads(path.read_text())
            self.runs[run["runId"]] = run
            if run["status"] == ACTIVE:
                evidence = path.parent / "evidence.json"
                if evidence.exists():
                    self.finish(run, json.loads(evidence.read_text()))
                else:
                    run.update(status="INTERRUPTED", phase="FINISHED", finishedAt=now(),
                               failureCode="SERVER_INTERRUPTED")
                    self.persist(run)

    def persist(self, run):
        save(self.directory / run["runId"] / "run.json", run)

    def snapshot(self, run):
        # Never hand the HTTP worker a mutable object being updated in background.
        return json.loads(json.dumps(run))

    def get(self, run_id):
        with self.lock:
            if run_id not in self.runs:
                raise ApiError(404, "RUN_NOT_FOUND", "Unknown runId")
            return self.snapshot(self.runs[run_id])

    def list(self):
        with self.lock:
            runs = sorted(self.runs.values(), key=lambda item: item["createdAt"], reverse=True)[:50]
            return {"items": [self.snapshot(run) for run in runs], "count": len(runs)}

    def start(self, key=None):
        with self.lock:
            key_hash = hashlib.sha256(key.encode()).hexdigest() if key else None
            if key_hash:
                for run in self.runs.values():
                    key_file = self.directory / run["runId"] / "idempotency.sha256"
                    if key_file.exists() and hmac.compare_digest(key_file.read_text(), key_hash):
                        return self.snapshot(run), True
            active = next((run for run in self.runs.values() if run["status"] == ACTIVE), None)
            if active:
                raise ApiError(409, "RUN_IN_PROGRESS", "Another MVP1 run is active",
                               runId=active["runId"], links=active["links"])
            run_id = str(uuid.uuid4())
            run_dir = self.directory / run_id
            run_dir.mkdir(mode=0o700)
            run = {"runId": run_id, "suite": "MVP1", "kind": "FUNCTIONAL_ACCEPTANCE",
                   "status": ACTIVE, "phase": "PREPARING", "createdAt": now(),
                   "startedAt": now(), "finishedAt": None,
                   "links": {"self": COLLECTION + "/" + run_id}}
            self.persist(run)
            if key_hash:
                (run_dir / "idempotency.sha256").write_text(key_hash)
            self.runs[run_id] = run
            self.worker = threading.Thread(target=self.execute, args=(run,), name="mvp1-runner")
            self.worker.start()
            return self.snapshot(run), False

    def progress(self, run, phase):
        with self.lock:
            run["phase"] = phase
            self.persist(run)

    def finish(self, run, evidence):
        checks = [{"name": check["name"], "passed": check.get("passed") is True}
                  for check in evidence.get("checks", [])]
        passed = sum(check["passed"] for check in checks)
        success = (evidence.get("passed") is True and evidence.get("cleanup_complete") is True
                   and evidence.get("existing_stack_preserved") is True and bool(checks)
                   and passed == len(checks))
        # Whitelist only. Raw evidence includes local paths, fixtures and diagnostics.
        run.update(status="SUCCEEDED" if success else "FAILED", phase="FINISHED", finishedAt=now(),
                   summary={"total": len(checks), "passed": passed, "failed": len(checks) - passed},
                   checks=checks, cleanupComplete=evidence.get("cleanup_complete") is True,
                   existingStackPreserved=evidence.get("existing_stack_preserved") is True,
                   sourceSha256=evidence.get("context", {}).get("source_sha256"))
        if not success:
            run["failureCode"] = "MVP1_VERIFICATION_FAILED"
        self.persist(run)

    def execute(self, run):
        try:
            evidence = self.runner(self.directory / run["runId"], lambda phase: self.progress(run, phase))
            with self.lock:
                self.finish(run, evidence)
        except Exception:
            with self.lock:
                run.update(status="FAILED", phase="FINISHED", finishedAt=now(),
                           failureCode="RUNNER_ERROR")
                self.persist(run)

    def close(self):
        # Graceful shutdown waits for the accepted run and its cleanup, not a kill.
        if self.worker:
            self.worker.join()
        self.process_lock.close()


class OperationsServer(ThreadingHTTPServer):
    daemon_threads = True

    def server_close(self):
        super().server_close()
        if hasattr(self, "store"):
            self.store.close()


class OperationsHandler(BaseHTTPRequestHandler):
    server_version = "PgLocalOps/1"

    def setup(self):
        super().setup()
        self.connection.settimeout(5)

    def log_message(self, *args):
        pass  # Do not log Authorization, idempotency keys or caller-controlled paths.

    def respond(self, status, body, headers=None):
        payload = json.dumps(body, ensure_ascii=False).encode()
        self.send_response(status)
        for name, value in {"Content-Type": "application/json; charset=utf-8",
                            "Content-Length": str(len(payload)), "Cache-Control": "no-store",
                            "X-Content-Type-Options": "nosniff", **(headers or {})}.items():
            self.send_header(name, value)
        self.end_headers()
        self.wfile.write(payload)

    def dispatch(self):
        try:
            if not hmac.compare_digest(self.headers.get("Authorization", ""), "Bearer " + self.server.token):
                raise ApiError(401, "UNAUTHORIZED", "An operations bearer token is required")
            if self.headers.get("Origin"):
                raise ApiError(403, "ORIGIN_FORBIDDEN", "Browser-origin requests are disabled")
            url = urlsplit(self.path)
            if url.query or url.fragment:
                raise ApiError(400, "INVALID_QUERY", "Query parameters are not supported")
            path = url.path.rstrip("/") if url.path == COLLECTION + "/" else url.path
            detail = re.fullmatch(re.escape(COLLECTION) + r"/([0-9a-f-]{36})", path)
            if path != COLLECTION and not detail:
                raise ApiError(404, "NOT_FOUND", "Unknown operations endpoint")
            if self.command == "GET":
                self.respond(200, self.server.store.get(detail[1]) if detail else self.server.store.list())
            elif self.command == "POST" and path == COLLECTION:
                if self.headers.get_content_type() != "application/json":
                    raise ApiError(415, "JSON_REQUIRED", "Use Content-Type: application/json")
                length = self.headers.get("Content-Length", "")
                if not length.isdecimal() or self.headers.get("Transfer-Encoding"):
                    raise ApiError(400, "INVALID_LENGTH", "A Content-Length is required")
                if not 0 < int(length) <= 1024:
                    raise ApiError(413, "BODY_TOO_LARGE", "Body must be between 1 and 1024 bytes")
                try:
                    body = json.loads(self.rfile.read(int(length)))
                except (ValueError, UnicodeError):
                    raise ApiError(400, "INVALID_JSON", "Malformed JSON")
                if body != {"suite": "MVP1"}:
                    raise ApiError(400, "INVALID_SUITE", 'Only {"suite":"MVP1"} is supported')
                key = self.headers.get("Idempotency-Key")
                if key is not None and not re.fullmatch(r"[A-Za-z0-9._:-]{1,128}", key):
                    raise ApiError(400, "INVALID_IDEMPOTENCY_KEY", "Use 1-128 ASCII letters, numbers or ._:-")
                run, replay = self.server.store.start(key)
                self.respond(200 if replay else 202, run, {"Location": run["links"]["self"], "Retry-After": "3"})
            else:
                raise ApiError(405, "METHOD_NOT_ALLOWED", "Use GET or POST on the collection; GET on a run")
        except ApiError as error:
            headers = {"WWW-Authenticate": 'Bearer realm="local-ops"'} if error.status == 401 else {}
            if error.status == 405:
                headers["Allow"] = "GET" if detail else "GET, POST"
            self.respond(error.status, error.body, headers)
        except (BrokenPipeError, ConnectionResetError):
            pass
        except TimeoutError:
            self.respond(408, {"code": "REQUEST_TIMEOUT", "message": "Request body timed out"})
        except Exception:
            self.respond(500, {"code": "INTERNAL_ERROR", "message": "Inspect private operations state"})

    do_GET = dispatch
    do_POST = dispatch
    do_PUT = dispatch
    do_DELETE = dispatch
    do_PATCH = dispatch
    do_OPTIONS = dispatch


def create_server(repo, directory, token, runner=None, port=38096):
    if not token:
        raise ValueError("Operations token cannot be empty")
    store = RunStore(repo, directory, runner)
    try:
        server = OperationsServer(("127.0.0.1", port), OperationsHandler)
    except Exception:
        store.close()
        raise
    server.store, server.token = store, token
    return server


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=38096)
    parser.add_argument("--state-dir", type=Path,
                        default=Path(tempfile_root()) / ("pg-platform-ops-" + str(os.getuid())))
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[2]
    directory = args.state_dir.expanduser().resolve()
    if directory == repo or repo in directory.parents:
        parser.error("Keep operations state and secrets outside the repository")
    directory.mkdir(mode=0o700, parents=True, exist_ok=True)
    os.chmod(directory, 0o700)
    token_path = directory / "ops-token"
    try:
        fd = os.open(token_path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(fd, "w") as output:
            output.write(secrets.token_urlsafe(32) + "\n")
    except FileExistsError:
        pass
    if token_path.stat().st_mode & 0o077:
        parser.error("Operations token must be private (chmod 600)")
    token = token_path.read_text().strip()
    with create_server(repo, directory, token, port=args.port) as server:
        print(f"Local operations API: http://127.0.0.1:{server.server_port}{COLLECTION}", flush=True)
        print(f"Bearer token file (private): {token_path}", flush=True)
        try:
            server.serve_forever()
        except KeyboardInterrupt:
            print("Waiting for the current run and Docker cleanup...", flush=True)


def tempfile_root():
    return "/private/tmp" if Path("/private/tmp").exists() else "/tmp"


if __name__ == "__main__":
    main()
