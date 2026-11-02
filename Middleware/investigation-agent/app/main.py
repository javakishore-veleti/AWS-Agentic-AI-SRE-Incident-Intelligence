from fastapi import FastAPI
from pydantic import BaseModel, Field

from app.graph import SERVICES, build_graph

app = FastAPI(title="Heddle investigation agent")
graph = build_graph()


class InvestigationRequest(BaseModel):
    service: str = Field(default="orders-service")
    start: str | None = None
    end: str | None = None


@app.get("/health")
def health() -> dict:
    return {"status": "UP"}


@app.get("/api/services")
def services() -> dict:
    return {"services": sorted(SERVICES)}


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
