export type EvidenceRow = {
  evidenceId: string
  source: string
  query: string
  retrievedAt: string
  items: unknown[]
  error: string | null
}

export type InvestigationReport = {
  service: string
  signals: Record<string, { status?: string }>
  facts: string[]
  hypotheses: string[]
  recommendations: string[]
  evidence: EvidenceRow[]
  uncertainty: string[]
  modelCalled: boolean
  modelId: string | null
  sample?: boolean
}

export const sampleReport: InvestigationReport = {
  service: "orders-service",
  signals: { "orders-service": { status: "UP" } },
  facts: [
    "Sample only. These rows were not retrieved from a live store.",
    "Trace 0123456789abcdef appears in ev-prometheus-sample, ev-jaeger-sample.",
  ],
  hypotheses: [
    "Retrieved rows are cited as ev-prometheus-sample, ev-jaeger-sample. A root cause is not assigned from these rows alone.",
    "unsupported. A second hypothesis cited no retrieved evidence.",
  ],
  recommendations: [
    "No change is recommended. A person approves any later change.",
  ],
  evidence: [
    {
      evidenceId: "ev-prometheus-sample",
      source: "prometheus",
      query: "sample",
      retrievedAt: "2026-11-02T05:00:00+00:00",
      items: [{ service: "orders-service" }],
      error: null,
    },
    {
      evidenceId: "ev-jaeger-sample",
      source: "jaeger",
      query: "sample",
      retrievedAt: "2026-11-02T05:00:00+00:00",
      items: [{ traceId: "0123456789abcdef" }],
      error: null,
    },
  ],
  uncertainty: ["No model was called.", "Correlation in this sample is illustrative."],
  modelCalled: false,
  modelId: null,
  sample: true,
}

export function reportFileName(report: InvestigationReport): string {
  return `investigation-${report.service}.json`
}

export function reportJson(report: InvestigationReport): string {
  return JSON.stringify(report, null, 2)
}
