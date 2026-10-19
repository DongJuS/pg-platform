"""HTTP contracts for the local operations API; Docker execution is the only fake."""
import json
from pathlib import Path
import tempfile
import threading
import time
import unittest
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from ops_api import create_server


class OperationsHttpTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.release = threading.Event()
        self.calls = 0

        def docker_runner(run_directory, progress):
            self.calls += 1
            progress("VERIFYING")
            self.release.wait(3)
            return {"passed": True, "cleanup_complete": True,
                    "existing_stack_preserved": True,
                    "context": {"source_sha256": "abc", "private_secret": "must-not-leak"},
                    "checks": [{"name": "Money is conserved", "passed": True,
                                "actual": {"token": "must-not-leak"}}]}

        self.runner = docker_runner
        self.start()

    def start(self):
        self.server = create_server(Path(__file__).parent.parents[1],
                                    Path(self.directory.name), "test-operator-token",
                                    runner=self.runner, port=0)
        self.thread = threading.Thread(target=self.server.serve_forever)
        self.thread.start()
        self.base = "http://127.0.0.1:" + str(self.server.server_port)

    def stop(self):
        self.release.set()
        self.server.shutdown()
        self.server.server_close()
        self.thread.join()

    def tearDown(self):
        self.stop()
        self.directory.cleanup()

    def http(self, method, path="/ops/v1/performance-runs", body=None, token=True, headers=None):
        request_headers = {"Content-Type": "application/json", **(headers or {})}
        if token:
            request_headers["Authorization"] = "Bearer test-operator-token"
        request = Request(self.base + path, method=method, headers=request_headers,
                          data=None if body is None else json.dumps(body).encode())
        try:
            response = urlopen(request, timeout=3)
        except HTTPError as error:
            response = error
        with response:
            return response.status, json.loads(response.read()), response.headers

    def completed(self, path):
        deadline = time.monotonic() + 3
        while time.monotonic() < deadline:
            status, result, _ = self.http("GET", path)
            if result["status"] != "RUNNING":
                return result
            time.sleep(.01)
        self.fail("Run did not finish")

    def test_post_accepts_and_get_reports_sanitized_persistent_result(self):
        status, accepted, headers = self.http("POST", body={"suite": "MVP1"})
        self.assertEqual(202, status)
        location = headers["Location"]
        self.assertEqual("RUNNING", accepted["status"])
        self.assertEqual(accepted["links"]["self"], location)
        self.release.set()
        result = self.completed(location)
        self.assertEqual("SUCCEEDED", result["status"])
        self.assertEqual({"total": 1, "passed": 1, "failed": 0}, result["summary"])
        self.assertNotIn("must-not-leak", json.dumps(result))
        self.stop()
        self.start()
        self.assertEqual(result, self.http("GET", location)[1])
        self.assertEqual(1, self.http("GET")[1]["count"])

    def test_same_key_replays_and_other_concurrent_run_is_rejected(self):
        first = self.http("POST", body={"suite": "MVP1"}, headers={"Idempotency-Key": "test:1"})
        replay = self.http("POST", body={"suite": "MVP1"}, headers={"Idempotency-Key": "test:1"})
        self.assertEqual(200, replay[0])
        self.assertEqual(first[1]["runId"], replay[1]["runId"])
        conflict = self.http("POST", body={"suite": "MVP1"}, headers={"Idempotency-Key": "test:2"})
        self.assertEqual(409, conflict[0])
        self.assertEqual(first[1]["runId"], conflict[1]["runId"])
        self.release.set()
        self.completed(first[2]["Location"])
        self.stop()
        self.start()
        replay = self.http("POST", body={"suite": "MVP1"}, headers={"Idempotency-Key": "test:1"})
        self.assertEqual(200, replay[0])
        self.assertEqual("SUCCEEDED", replay[1]["status"])
        self.assertEqual(1, self.calls)

    def test_get_and_post_require_a_separate_operations_token(self):
        for method, body in [("GET", None), ("POST", {"suite": "MVP1"})]:
            with self.subTest(method=method):
                response = self.http(method, body=body, token=False)
                self.assertEqual(401, response[0])
                self.assertIn("WWW-Authenticate", response[2])
        self.assertEqual(0, self.calls)

    def test_invalid_payload_or_browser_origin_cannot_start_a_run(self):
        for body in ({"suite": "MVP2"}, {"suite": "MVP1", "command": "rm"}, [], None):
            with self.subTest(body=body):
                self.assertEqual(400 if body is not None else 413,
                                 self.http("POST", body=body)[0])
        self.assertEqual(403, self.http("POST", body={"suite": "MVP1"}, headers={"Origin": "https://example.com"})[0])
        self.assertEqual(415, self.http("POST", body={"suite": "MVP1"}, headers={"Content-Type": "text/plain"})[0])
        self.assertEqual(413, self.http("POST", body={"suite": "x" * 1100})[0])
        self.assertEqual(400, self.http("POST", body={"suite": "MVP1"}, headers={"Idempotency-Key": "bad key"})[0])
        self.assertEqual(0, self.calls)

    def test_unknown_run_and_wrong_method_have_http_errors(self):
        self.assertEqual(404, self.http("GET", "/ops/v1/performance-runs/00000000-0000-0000-0000-000000000000")[0])
        self.assertEqual(404, self.http("GET", "/ops/v1/performance-runs/../../ops-token")[0])
        response = self.http("DELETE")
        self.assertEqual(405, response[0])
        self.assertEqual("GET, POST", response[2]["Allow"])

    def test_failed_checks_are_reported_without_claiming_success(self):
        self.stop()
        self.runner = lambda directory, progress: {
            "passed": False, "cleanup_complete": True, "existing_stack_preserved": True,
            "checks": [{"name": "OTP replay rejected", "passed": False}]}
        self.start()
        response = self.http("POST", body={"suite": "MVP1"})
        result = self.completed(response[2]["Location"])
        self.assertEqual("FAILED", result["status"])
        self.assertEqual({"total": 1, "passed": 0, "failed": 1}, result["summary"])
        self.assertTrue(result["cleanupComplete"])

    def test_cleanup_failure_does_not_report_success(self):
        self.stop()
        self.runner = lambda directory, progress: {
            "passed": True, "cleanup_complete": False, "existing_stack_preserved": True,
            "checks": [{"name": "Transfer completed", "passed": True}]}
        self.start()
        response = self.http("POST", body={"suite": "MVP1"})
        result = self.completed(response[2]["Location"])
        self.assertEqual("FAILED", result["status"])
        self.assertFalse(result["cleanupComplete"])

    def test_runner_exception_is_sanitized_and_allows_a_new_run(self):
        self.stop()

        def broken(directory, progress):
            raise RuntimeError("secret password must-not-leak")

        self.runner = broken
        self.start()
        response = self.http("POST", body={"suite": "MVP1"})
        result = self.completed(response[2]["Location"])
        self.assertEqual("RUNNER_ERROR", result["failureCode"])
        self.assertNotIn("must-not-leak", json.dumps(result))
        self.assertEqual(202, self.http("POST", body={"suite": "MVP1"})[0])

    def test_second_server_cannot_bypass_single_active_run(self):
        with self.assertRaisesRegex(RuntimeError, "already active"):
            create_server(Path(__file__).parent.parents[1], Path(self.directory.name),
                          "test-token", runner=self.runner, port=0)


if __name__ == "__main__":
    unittest.main()
