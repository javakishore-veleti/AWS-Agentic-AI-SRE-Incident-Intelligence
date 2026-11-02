import json
import os
import re
import urllib.error
import urllib.parse
import urllib.request
import uuid
from datetime import datetime, timedelta, timezone

MAX_LIMIT = 20
MAX_WINDOW = timedelta(hours=6)
TIMEOUT_SECONDS = 3
SOURCES = ("prometheus", "elasticsearch", "jaeger", "deployments")
_TOKEN = re.compile(r"(?i)(bearer\s+)[A-Za-z0-9._~+/-]+=*")
_JWT = re.compile(r"eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}")


def query_evidence(
    source: str,
    service: str,
    start: str,
    end: str,
    limit: int = MAX_LIMIT,
    trace_id: str | None = None,
    statement: str | None = None,
    opener=None,
) -> dict:
    retrieved = datetime.now(timezone.utc).isoformat()
    evidence_id = f"ev-{source}-{uuid.uuid4().hex[:8]}"
    try:
        request = _request(source, service, start, end, limit, trace_id, statement)
    except ValueError as exc:
        return _empty(evidence_id, source, service, "", retrieved, str(exc))
    base = os.environ.get(_env_name(source), "").strip()
    if not base:
        return _empty(evidence_id, source, service, request.full_url if hasattr(request, "full_url") else "", retrieved, f"{source} is not configured")
    if request.get_method() != "GET":
        return _empty(evidence_id, source, service, request.full_url, retrieved, "only GET is allowed")
    fetch = opener or urllib.request.urlopen
    try:
        with fetch(request, timeout=TIMEOUT_SECONDS) as response:
            raw = response.read().decode("utf-8")
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        return _empty(evidence_id, source, service, request.full_url, retrieved, str(exc))
    try:
        payload = json.loads(raw)
    except json.JSONDecodeError:
        return _empty(evidence_id, source, service, request.full_url, retrieved, "response was not JSON")
    return {
        "evidenceId": evidence_id,
        "source": source,
        "service": service,
        "query": request.full_url,
        "retrievedAt": retrieved,
        "items": _items(source, payload)[:limit],
        "error": None,
    }


def _request(source, service, start, end, limit, trace_id, statement):
    if source not in SOURCES:
        raise ValueError(f"unknown source {source}")
    if not service or len(service) > 80:
        raise ValueError("service is required")
    window_start, window_end = _window(start, end)
    if limit < 1 or limit > MAX_LIMIT:
        raise ValueError(f"limit must be from 1 to {MAX_LIMIT}")
    base = os.environ.get(_env_name(source), "http://127.0.0.1").rstrip("/")
    if statement and (
        not re.fullmatch(r"[A-Za-z0-9_.:\"{}=,()\[\] ]{1,240}", statement)
        or re.search(r"(?i)\b(delete|drop|insert|update|alter|truncate)\b", statement)
    ):
        raise ValueError("statement is not a bounded read query")
    params = {
        "service": service,
        "start": window_start.isoformat(),
        "end": window_end.isoformat(),
        "limit": str(limit),
    }
    if trace_id:
        if not re.fullmatch(r"[A-Fa-f0-9]{16,32}", trace_id):
            raise ValueError("trace id is not a hex identifier")
        params["traceID"] = trace_id
    path = {
        "prometheus": "/api/v1/query_range",
        "elasticsearch": "/sre-services-*/_search",
        "jaeger": "/api/traces",
        "deployments": "/deployments",
    }[source]
    if source == "prometheus":
        params["query"] = statement or f'http_server_requests_seconds_count{{service="{service}"}}'
    query = urllib.parse.urlencode(params)
    url = f"{base}{path}?{query}"
    parsed = urllib.parse.urlparse(url)
    if parsed.scheme not in {"http", "https"} or parsed.username:
        raise ValueError("source URL must be http or https without credentials")
    return urllib.request.Request(url, method="GET")


def _window(start: str, end: str):
    opened = datetime.fromisoformat(start)
    closed = datetime.fromisoformat(end)
    if opened.tzinfo is None or closed.tzinfo is None:
        raise ValueError("start and end need a timezone")
    if closed <= opened or closed - opened > MAX_WINDOW:
        raise ValueError("time window must be positive and at most 6 hours")
    return opened, closed


def _env_name(source: str) -> str:
    return {
        "prometheus": "PROMETHEUS_URL",
        "elasticsearch": "ELASTICSEARCH_URL",
        "jaeger": "JAEGER_URL",
        "deployments": "DEPLOYMENT_HISTORY_URL",
    }[source]


def _items(source: str, payload: dict) -> list:
    if source == "prometheus":
        return payload.get("data", {}).get("result", [])
    if source == "elasticsearch":
        hits = payload.get("hits", {}).get("hits", [])
        return [_redact(hit) for hit in hits]
    if source == "jaeger":
        return payload.get("data", [])
    return payload.get("items", [])


def _redact(value):
    if isinstance(value, str):
        cleaned = _TOKEN.sub(r"\1[redacted]", value)
        return _JWT.sub("[redacted]", cleaned)
    if isinstance(value, list):
        return [_redact(item) for item in value]
    if isinstance(value, dict):
        return {key: _redact(item) for key, item in value.items()}
    return value


def _empty(evidence_id, source, service, query, retrieved, error):
    return {
        "evidenceId": evidence_id,
        "source": source,
        "service": service,
        "query": query,
        "retrievedAt": retrieved,
        "items": [],
        "error": error,
    }
