from datetime import datetime, timezone

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from app.graph import SERVICES, build_graph
from app.guardrails import record_approval

app = FastAPI(title="Heddle investigation agent")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://127.0.0.1:5173", "http://localhost:5173"],
    allow_methods=["GET", "POST"],
    allow_headers=["Content-Type"],
)
graph = build_graph()


class InvestigationRequest(BaseModel):
    service: str = Field(default="orders-service")
    start: str | None = None
    end: str | None = None


class ApprovalRequest(BaseModel):
    approver: str = ""
    ticket: str = ""
    environment: str = ""
    expiresAt: str = ""
    change: str = ""
    evidenceIds: list[str] = Field(default_factory=list)
    executed: bool = False


@app.get("/health")
def health() -> dict:
    return {"status": "UP"}


@app.get("/api/services")
def services() -> dict:
    return {"services": sorted(SERVICES)}


@app.post("/api/approvals")
def approvals(request: ApprovalRequest) -> dict:
    return record_approval(request.model_dump(), datetime.now(timezone.utc))


@app.post("/api/investigations")
def investigate(request: InvestigationRequest) -> dict:
    result = graph.invoke(_blank(request.service, request.start, request.end))
    return {
        "service": result["service"],
        "signals": result["signals"],
        "facts": result["facts"],
        "hypotheses": result.get("report_hypotheses") or result["hypotheses"],
        "recommendations": result["recommendations"],
        "evidence": result["evidence"],
        "uncertainty": result["uncertainty"],
        "modelCalled": result["model_called"],
        "modelId": result["model_id"],
    }


def _blank(service: str, start: str | None, end: str | None) -> dict:
    return {
        "service": service,
        "start": start or "",
        "end": end or "",
        "signals": {},
        "facts": [],
        "hypotheses": [],
        "recommendations": [],
        "evidence": [],
        "uncertainty": [],
        "report_hypotheses": [],
        "model_called": False,
        "model_id": None,
    }
