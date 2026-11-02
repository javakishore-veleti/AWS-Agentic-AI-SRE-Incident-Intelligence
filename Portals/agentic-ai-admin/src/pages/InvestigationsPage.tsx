import { useState } from "react"
import { runInvestigation } from "../api"
import { reportFileName, reportJson, sampleReport, type InvestigationReport } from "../report"

const services = [
  "product-catalog-service",
  "offers-service",
  "customers-service",
  "orders-service",
  "inventory-service",
]

export function InvestigationsPage() {
  const [service, setService] = useState("orders-service")
  const [report, setReport] = useState<InvestigationReport | null>(null)
  const [error, setError] = useState("")
  const [pending, setPending] = useState(false)
  const [downloadUrl, setDownloadUrl] = useState("")

  async function investigate() {
    setPending(true)
    setError("")
    try {
      const next = await runInvestigation(service)
      setReport(next)
      setDownloadUrl("")
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "The investigation could not be read.")
    } finally {
      setPending(false)
    }
  }

  function showSample() {
    setError("")
    setReport(sampleReport)
    setDownloadUrl("")
  }

  function exportReport() {
    if (!report) return
    const blob = new Blob([reportJson(report)], { type: "application/json" })
    const url = URL.createObjectURL(blob)
    setDownloadUrl(url)
  }

  return (
    <>
      <p className="crumb">Desk / Investigations</p>
      <div className="page-head">
        <div>
          <h1>Investigations</h1>
          <p className="lede">Read a report, then export it. The desk does not apply a change.</p>
        </div>
      </div>
      <div className="card">
        <div className="form-actions">
          <label className="field">
            Service
            <select value={service} onChange={(event) => setService(event.target.value)}>
              {services.map((name) => (
                <option key={name} value={name}>{name}</option>
              ))}
            </select>
          </label>
          <button className="primary" type="button" onClick={investigate} disabled={pending}>
            {pending ? "Reading" : "Run investigation"}
          </button>
          <button className="text-btn" type="button" onClick={showSample}>Open sample report</button>
        </div>
      </div>
      {error ? <div className="banner warn">{error}</div> : null}
      {report ? <ReportView report={report} downloadUrl={downloadUrl} onExport={exportReport} /> : <p className="empty">No report is open.</p>}
    </>
  )
}

function ReportView({
  report,
  downloadUrl,
  onExport,
}: {
  report: InvestigationReport
  downloadUrl: string
  onExport: () => void
}) {
  return (
    <>
      {report.sample ? <div className="banner warn">Sample. These rows were not retrieved from a live store.</div> : null}
      <section>
        <h2>Timeline</h2>
        <ul>
          {report.facts.map((fact) => <li key={fact}>{fact}</li>)}
        </ul>
      </section>
      <section>
        <h2>Signals</h2>
        <div className="table-wrap card">
          <table>
            <thead><tr><th>Service</th><th>Status</th></tr></thead>
            <tbody>
              {Object.entries(report.signals).map(([name, signal]) => (
                <tr key={name}><td>{name}</td><td>{signal.status ?? "unknown"}</td></tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      <section>
        <h2>Hypotheses</h2>
        <p className="muted">Confidence is not calibrated.</p>
        <ul>
          {report.hypotheses.map((item) => <li key={item}>{item}</li>)}
        </ul>
      </section>
      <section>
        <h2>Uncertainty</h2>
        <ul>
          {report.uncertainty.map((item) => <li key={item}>{item}</li>)}
        </ul>
      </section>
      <section>
        <h2>Suggested remediation</h2>
        <ul>
          {report.recommendations.map((item) => <li key={item}>{item}</li>)}
        </ul>
      </section>
      <section>
        <h2>Evidence</h2>
        <div className="table-wrap card">
          <table>
            <thead><tr><th>Id</th><th>Source</th><th>Retrieved</th><th>Rows</th><th>Error</th></tr></thead>
            <tbody>
              {report.evidence.map((row) => (
                <tr key={row.evidenceId}>
                  <td>{row.evidenceId}</td>
                  <td>{row.source}</td>
                  <td>{row.retrievedAt}</td>
                  <td>{row.items.length}</td>
                  <td>{row.error ?? ""}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      <div className="form-actions">
        <button className="primary" type="button" onClick={onExport}>Export report</button>
        {downloadUrl ? (
          <a href={downloadUrl} download={reportFileName(report)}>Download {reportFileName(report)}</a>
        ) : null}
      </div>
      <p className="muted">Model called: {report.modelCalled ? "yes" : "no"}{report.modelId ? ` (${report.modelId})` : ""}</p>
    </>
  )
}
