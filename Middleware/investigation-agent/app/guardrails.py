import re
from datetime import datetime

ALLOWED_TOOLS = frozenset({"health", "prometheus", "elasticsearch", "jaeger", "deployments"})
MAX_TOOL_CALLS = 12
_INJECTION = re.compile(r"(?i)(ignore previous|system prompt|you are now|run this command)")
_DANGEROUS = re.compile(r"(?i)\b(kubectl|shell|sudo|rm|delete|drop|terraform)\b")
_SAFE = "No change is recommended. Retrieved evidence does not support a cause. A person approves any later change."


def allow_tool(name: str) -> None:
    if name not in ALLOWED_TOOLS:
        raise PermissionError(f"{name} is not an allowed read")


def note_call(calls: int) -> int:
    calls += 1
    if calls > MAX_TOOL_CALLS:
        raise PermissionError(f"tool call budget is {MAX_TOOL_CALLS}")
    return calls


def scrub(value):
    if isinstance(value, str):
        if _INJECTION.search(value):
            return "[untrusted telemetry omitted]"
        return value
    if isinstance(value, list):
        return [scrub(item) for item in value]
    if isinstance(value, dict):
        return {key: scrub(item) for key, item in value.items()}
    return value


def rejected_text(text: str) -> bool:
    return bool(_INJECTION.search(text) or _DANGEROUS.search(text))


def safe_recommendation(text: str, evidence_ids: list[str]) -> str:
    if not evidence_ids or rejected_text(text):
        return _SAFE
    return text


def record_approval(approval: dict, now: datetime) -> dict:
    denied = {"approved": False, "executed": False}
    if not isinstance(approval, dict):
        return {**denied, "reason": "approval is incomplete"}
    if approval.get("executed") is True:
        return {**denied, "reason": "an approval cannot authorize this agent to make a change"}
    required = ("approver", "ticket", "environment", "expiresAt", "change")
    if any(not str(approval.get(key) or "").strip() for key in required):
        return {**denied, "reason": "approval is incomplete"}
    try:
        expires = datetime.fromisoformat(str(approval["expiresAt"]))
    except ValueError:
        return {**denied, "reason": "approval expiry is not a time"}
    if expires.tzinfo is None or expires <= now:
        return {**denied, "reason": "approval is expired"}
    evidence_ids = approval.get("evidenceIds") or []
    if _DANGEROUS.search(str(approval["change"])) and not evidence_ids:
        return {**denied, "reason": "a change needs retrieved evidence"}
    return {
        "approved": True,
        "executed": False,
        "reason": "recorded for a person. This agent does not apply the change.",
    }
