"""Disposable MVP5 test-only proxy: drop an actual committed Ledger HTTP response.
No host port, credentials, request bodies or personal data are stored or logged.
"""
from decimal import Decimal
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import socket
import threading
import urllib.error
import urllib.request

lock = threading.Lock()
state = {'enabled': True, 'dropped_reservation_responses': 0}
class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args): pass
    def reply(self, status, payload):
        self.send_response(status); self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(payload))); self.end_headers(); self.wfile.write(payload)
    def do_GET(self): self.handle_request()
    def do_POST(self): self.handle_request()
    def handle_request(self):
        size = int(self.headers.get('Content-Length', '0'))
        if size < 0 or size > 65536: return self.reply(413, b'{}')
        if self.headers.get('Transfer-Encoding', '').lower() == 'chunked':
            chunks, total = [], 0
            while True:
                chunk_size = int(self.rfile.readline(128).split(b';', 1)[0].strip(), 16)
                if chunk_size == 0:
                    while self.rfile.readline(4096).strip(): pass
                    break
                total += chunk_size
                if chunk_size < 0 or total > 65536: return self.reply(413, b'{}')
                chunks.append(self.rfile.read(chunk_size))
                if self.rfile.read(2) != b'\r\n': return self.reply(400, b'{}')
            body = b''.join(chunks)
        else:
            body = self.rfile.read(size) if size else None
        if self.path == '/__faults':
            with lock:
                if self.command == 'POST': state['enabled'] = bool(json.loads(body)['enabled'])
                value = json.dumps(state).encode()
            return self.reply(200, value)
        if self.path == '/health': return self.reply(200, b'{}')
        if not self.path.startswith('/internal/v1/ledger/'): return self.reply(404, b'{}')
        headers = {key: self.headers[key] for key in ['Content-Type', 'X-Internal-Caller', 'X-Internal-Service-Token'] if self.headers.get(key)}
        request = urllib.request.Request('http://ledger:8083' + self.path, data=body, headers=headers, method=self.command)
        try:
            with urllib.request.urlopen(request, timeout=10) as response: status, payload = response.status, response.read()
        except urllib.error.HTTPError as error: status, payload = error.code, error.read()
        except (urllib.error.URLError, TimeoutError): return self.reply(503, b'{}')
        is_reservation = self.path == '/internal/v1/ledger/overseas-reservations' and body is not None
        drop = False
        if is_reservation and status == 200:
            try: matches = Decimal(str(json.loads(body).get('amount'))) == Decimal('17.00')
            except Exception: matches = False
            with lock:
                drop = state['enabled'] and matches
                if drop: state['dropped_reservation_responses'] += 1
        if drop:
            # The upstream returned HTTP 200 only after its real MariaDB commit. Send no downstream response.
            self.close_connection = True
            try: self.connection.shutdown(socket.SHUT_RDWR)
            except OSError: pass
            self.connection.close()
            return
        try: self.reply(status, payload)
        except (BrokenPipeError, ConnectionResetError): pass
ThreadingHTTPServer(('0.0.0.0', 18083), Handler).serve_forever()
