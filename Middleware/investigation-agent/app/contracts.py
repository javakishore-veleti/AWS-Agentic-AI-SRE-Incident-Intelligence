import re

from pydantic import BaseModel, Field

MAX_RETRIES = 1
EVIDENCE_ID = re.compile(r"^ev-(prometheus|elasticsearch|jaeger|deployments)-[0-9a-f]{8}$")


class EvidenceContract(BaseModel):
    evidenceId: str
    source: str
    service: str
    query: str
    retrievedAt: str
    items: list = Field(default_factory=list)
    error: str | None = None


class ReportContract(BaseModel):
    service: str
    signals: dict
    facts: list[str]
    hypotheses: list[str]
    recommendations: list[str]
    evidence: list[EvidenceContract]
    uncertainty: list[str]
    modelCalled: bool
    modelId: str | None = None

    def checked(self) -> "ReportContract":
        for row in self.evidence:
            if not EVIDENCE_ID.match(row.evidenceId):
                raise ValueError(f"evidence id {row.evidenceId} is not valid")
        if any(row.error for row in self.evidence) and not self.uncertainty:
            raise ValueError("a failed read must be listed as uncertainty")
        return self
