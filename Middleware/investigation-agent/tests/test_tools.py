import json
import os
from datetime import datetime, timedelta, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from threading import Thread

import pytest

from app.tools import query_evidence


class _Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if "Bearer" in self.path or self.command != "GET":
            self.send_error(400)
            return
        body = json.dumps(self.server.payload).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_POST(self):
        self.send_error(405)

    def log_message(self, format, *args):
        return


@pytest.fixture()
def server():
    httpd = ThreadingHTTPServer(("127.0.0.1", 0), _Handler)
    httpd.payload = {"data": {"result": [{"metric": {"service": "orders-service"}, "value": [1, "2"]}]}}
    thread = Thread(target=httpd.serve_forever, daemon=True)
    thread.start()
    yield httpd
    httpd.shutdown()


def _window():
    end = datetime.now(timezone.utc)
    start = end - timedelta(minutes=15)
    return start.isoformat(), end.isoformat()


def test_prometheus_get_returns_timestamped_evidence(server, monkeypatch):
    monkeypatch.setenv("PROMETHEUS_URL", f"http://127.0.0.1:{server.server_port}")
    start, end = _window()
    evidence = query_evidence("prometheus", "orders-service", start, end, limit=5)
    assert evidence["error"] is None
    assert evidence["evidenceId"].startswith("ev-prometheus-")
    assert evidence["items"][0]["metric"]["service"] == "orders-service"
    assert "/api/v1/query_range?" in evidence["query"]
    assert evidence["retrievedAt"]


def test_missing_source_is_an_error_not_invented_rows(monkeypatch):
    monkeypatch.delenv("JAEGER_URL", raising=False)
    start, end = _window()
    evidence = query_evidence("jaeger", "orders-service", start, end)
    assert evidence["items"] == []
    assert "not configured" in evidence["error"]


def test_limit_and_window_are_bounded(monkeypatch):
    monkeypatch.setenv("PROMETHEUS_URL", "http://127.0.0.1:9")
    start, end = _window()
    too_many = query_evidence("prometheus", "orders-service", start, end, limit=21)
    assert "limit" in too_many["error"]
    opened = datetime.now(timezone.utc) - timedelta(hours=7)
    too_wide = query_evidence("prometheus", "orders-service", opened.isoformat(), datetime.now(timezone.utc).isoformat())
    assert "6 hours" in too_wide["error"]


def test_elasticsearch_redacts_a_bearer_token(monkeypatch):
    monkeypatch.setenv("ELASTICSEARCH_URL", "http://127.0.0.1:9")

    class Response:
        def __enter__(self):
            return self

        def __exit__(self, *args):
            return False

        def read(self):
            hit = {"_source": {"message": "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature"}}
            return json.dumps({"hits": {"hits": [hit]}}).encode("utf-8")

    def opener(request, timeout):
        assert request.get_method() == "GET"
        assert timeout == 3
        return Response()

    start, end = _window()
    evidence = query_evidence("elasticsearch", "orders-service", start, end, opener=opener)
    message = evidence["items"][0]["_source"]["message"]
    assert "eyJ" not in message
    assert "[redacted]" in message


def test_unknown_source_and_mutation_statement_are_rejected(monkeypatch):
    monkeypatch.setenv("PROMETHEUS_URL", "http://127.0.0.1:9")
    start, end = _window()
    unknown = query_evidence("shell", "orders-service", start, end)
    assert "unknown source" in unknown["error"]
    mutated = query_evidence("prometheus", "orders-service", start, end, statement="delete from orders")
    assert "bounded read" in mutated["error"]
    assert os.environ["PROMETHEUS_URL"]
