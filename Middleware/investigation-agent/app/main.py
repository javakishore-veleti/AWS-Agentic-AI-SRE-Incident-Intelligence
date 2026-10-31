from fastapi import FastAPI
from pydantic import BaseModel, Field

from app.graph import SERVICES, build_graph

app = FastAPI(title="Heddle investigation agent")
graph = build_graph()


class InvestigationRequest(BaseModel):
    service: str = Field(default="orders-service")


@app.get("/health")
def health() -> dict:
    return {"status": "UP"}


@app.get("/api/services")
def services() -> dict:
    return {"services": sorted(SERVICES)}


@app.post("/api/investigations")
def investigate(request: InvestigationRequest) -> dict:
    result = graph.invoke(
        {
            "service": request.service,
            "signals": {},
            "facts": [],
            "hypotheses": [],
            "recommendations": [],
        }
    )
    return {
        "service": result["service"],
        "signals": result["signals"],
        "facts": result["facts"],
        "hypotheses": result["hypotheses"],
        "recommendations": result["recommendations"],
    }
