# AWS Agentic AI for SRE Incident Intelligence

### Intelligent Incident Investigation, Root-Cause Analysis, and Remediation Decision Support

**Java Spring Boot | AWS EKS | OpenTelemetry | Amazon CloudWatch | LangGraph | Large Language Models**

## Overview

Modern enterprise applications operate across distributed microservices, Kubernetes clusters, databases, messaging systems, and cloud infrastructure.

When production incidents occur, Site Reliability Engineers (SREs) must investigate information distributed across multiple observability and operational platforms.

Although traditional monitoring tools detect anomalies and generate alerts, establishing the underlying cause of an incident often requires correlating application behavior, infrastructure metrics, distributed traces, deployment history, and service dependencies.

**AWS Agentic AI SRE Incident Intelligence** is a hands-on engineering project exploring how AI agents can assist Site Reliability Engineers with evidence-based incident investigation and operational decision-making.

The solution is designed to investigate production-like incidents using real telemetry, coordinate diagnostic activities across AWS services, evaluate competing root-cause hypotheses, and recommend remediation actions subject to operational safeguards.

The project also evaluates whether advanced reasoning models provide measurable advantages over faster, lower-cost models for complex SRE workloads.

> **Core principle:** AI should assist engineers in establishing what failed, why it failed, and what evidence supports the diagnosis. Production changes must remain governed by explicit operational controls.

## The Problem We Are Solving

Consider a Java Spring Boot microservice running on Amazon EKS.

During normal operations, its API response latency is approximately 200 milliseconds. Following a deployment, latency increases significantly, and some requests begin failing.

Initial observations indicate:

| Operational Signal | Observation |
|---|---|
| API latency | Increased from approximately 200 ms to several seconds |
| Kubernetes pods | Running and passing health checks |
| CPU utilization | Within normal operating limits |
| Memory utilization | Within normal operating limits |
| Database connections | Connection pool utilization approaching capacity |
| Application logs | Intermittent connection acquisition timeouts |
| Deployment history | Recent application configuration changes |

A conventional monitoring dashboard can expose these symptoms.

However, the underlying problem may involve a database connection leak, increased request concurrency, slow SQL queries, downstream service delays, or an incorrect application configuration.

The SRE must determine which explanation is supported by the evidence.

This is the problem the Agentic AI investigation workflow is designed to address.

## Solution Objectives

The project focuses on six engineering objectives:

1. **Incident context acquisition:** Collect relevant logs, metrics, traces, Kubernetes events, and deployment changes.
2. **Cross-system correlation:** Analyze relationships between application symptoms, infrastructure behavior, and service dependencies.
3. **Root-cause investigation:** Generate and validate competing explanations using operational evidence.
4. **Remediation decision support:** Recommend recovery actions with supporting evidence and potential risks.
5. **Operational safety:** Enforce read-only investigation permissions and human approval for production-changing actions.
6. **Model evaluation:** Compare AI models using investigation accuracy, latency, tool efficiency, operational safety, and cost.

The goal is not to replace existing observability platforms. It is to improve how engineers investigate and interpret the information those platforms already provide.

## Reference Architecture

The proposed architecture combines Java microservices running on AWS with an AI investigation workflow.

| Architecture Layer | Technology | Responsibility |
|---|---|---|
| Application services | Java 21, Spring Boot | Production-like microservice workloads |
| Container orchestration | Amazon EKS | Deploy and operate containerized services |
| Database | PostgreSQL / Amazon RDS | Persistent data and database connection monitoring |
| Application observability | OpenTelemetry | Generate distributed traces, metrics, and contextual telemetry |
| Monitoring and logging | Amazon CloudWatch | Application logs, operational metrics, and alerts |
| Trace analysis | OpenTelemetry-compatible trace backend | Investigate cross-service latency and failures |
| Agent orchestration | LangGraph | Coordinate multi-step investigations and agent execution |
| AI reasoning | Supported LLM APIs | Hypothesis generation, evidence interpretation, and investigation summaries |
| AWS integration | AWS SDK and controlled APIs | Retrieve authorized operational information |
| Load testing | k6 | Generate repeatable workload conditions |
| Evaluation | Structured test cases and reports | Measure diagnosis quality, latency, safety, and inference cost |

The investigation agent receives an incident context and uses authorized tools to retrieve telemetry.

It then evaluates possible causes, requests additional evidence where necessary, and produces a structured investigation report.

**The LLM does not independently monitor AWS, obtain unrestricted infrastructure access, or execute production changes.** Those capabilities depend on the tools, permissions, and controls explicitly implemented around it.

## Agentic Incident Investigation Workflow

The proposed workflow uses specialized investigation responsibilities coordinated through LangGraph.

| Agent Responsibility | Investigation Activity |
|---|---|
| Incident Triage | Interpret alerts, identify impacted services, and establish incident scope |
| Application Log Analysis | Identify errors, exceptions, timeouts, and relevant log patterns |
| Metrics Analysis | Examine latency, throughput, error rates, connection pools, and resource utilization |
| Distributed Trace Analysis | Investigate service-to-service communication and latency contributors |
| Kubernetes Investigation | Inspect pod status, restarts, resource constraints, and Kubernetes events |
| Change Correlation | Review deployments, configuration modifications, and their timing |
| Root-Cause Assessment | Evaluate competing hypotheses against collected evidence |
| Remediation Recommendation | Recommend corrective actions and identify operational risks |

These are logical responsibilities, not a requirement to run eight independent LLMs. The orchestration may use specialized tools, deterministic processing, and selected model calls depending on the investigation.

### Evidence-Based Reasoning

The system should distinguish between:

- **Observed facts:** Directly supported by logs, metrics, traces, or configuration records.
- **Hypotheses:** Possible explanations requiring additional investigation.
- **Validated findings:** Conclusions supported by sufficient operational evidence.
- **Recommendations:** Proposed recovery actions requiring appropriate authorization.

The system must not present an unverified hypothesis as an established root cause.

## Hands-On Incident Scenarios

The project will use controlled failures in Java microservices to test the AI investigation workflow.

### Scenario 1: Database Connection Pool Exhaustion

**Incident:** API latency increases while Kubernetes pods remain healthy.

**Fault injection:** Reduce the HikariCP maximum connection pool size while applying concurrent API traffic.

**Investigation:** Correlate connection acquisition waits, application timeouts, request latency, database activity, and recent configuration changes.

**Expected result:** Identify whether the connection pool configuration is responsible for the observed degradation.

**Validation:** Restore the previous configuration and verify that the affected performance metrics recover under comparable workload conditions.

### Scenario 2: Downstream Microservice Latency

**Incident:** Order Service experiences increased response times while its own CPU and memory usage remain normal.

**Fault injection:** Introduce a controlled response delay in Inventory Service.

**Investigation:** Analyze distributed traces, dependency calls, timeout behavior, and service-level latency.

**Expected result:** Identify the downstream service contributing to the increased end-to-end response time.

**Validation:** Remove the injected delay and confirm latency recovery.

### Scenario 3: Deployment Configuration Failure

**Incident:** Requests begin failing immediately following an application deployment.

**Fault injection:** Introduce an incorrect downstream service endpoint in the application configuration.

**Investigation:** Correlate request failures, network or application errors, deployment timestamps, and configuration differences.

**Expected result:** Identify the incorrect configuration as the cause of the failures.

**Validation:** Restore the correct configuration and confirm successful service communication.

### Scenario 4: Conflicting Operational Signals

**Incident:** Application error rates increase despite apparently healthy infrastructure metrics.

**Fault injection:** Introduce an application-level failure that does not cause Kubernetes health checks to fail.

**Investigation:** Compare application errors, health-check behavior, traces, and dependency responses.

**Expected result:** Distinguish application correctness failures from infrastructure availability.

**Validation:** Remove the injected fault and verify the reduction in application errors.

Each scenario must have a known ground-truth cause so that AI diagnoses can be evaluated objectively.

## Why Evaluate Different LLMs?

Not every operational incident requires the same level of AI reasoning.

Basic log classification and incident summarization may be handled adequately by faster, lower-cost models.

More complicated investigations may require multiple diagnostic steps, competing hypotheses, additional tool calls, and correlation across several systems.

However, greater model capability does not automatically guarantee better SRE outcomes.

This project will compare available models under equivalent conditions rather than assume that the most advanced model is always the best choice.

### Model Evaluation Criteria

| Evaluation Dimension | Measurement |
|---|---|
| Root-cause accuracy | Whether the model identifies the known fault |
| Evidence quality | Whether findings reference actual operational evidence |
| False conclusions | Unsupported or incorrect root-cause assertions |
| Investigation latency | Time required to produce a completed diagnosis |
| Tool efficiency | Number and relevance of diagnostic tool calls |
| Reasoning consistency | Stability of findings across repeated trials |
| Remediation quality | Technical correctness and safety of recommendations |
| Cost efficiency | Inference and tool-execution cost per investigation |
| Operational safety | Unauthorized actions or unsafe recommendations |

All candidate models should receive equivalent incident conditions, tool access, telemetry, and evaluation criteria.

Model-specific claims will be based on measured results, not marketing descriptions or unrelated benchmarks.

## Production Safety and Governance

The project treats production safety as an architectural requirement.

| Control | Design Approach |
|---|---|
| Authentication | IAM-based access to AWS services |
| Authorization | Least-privilege permissions for investigation tools |
| Read-only diagnostics | Restrict agent access to approved telemetry and configuration data |
| Sensitive data protection | Redact secrets and personal data before model invocation |
| Prompt injection defense | Treat logs, alerts, and external tool responses as untrusted data |
| Tool execution boundaries | Allowlist supported actions and validate their parameters |
| Human approval | Require explicit authorization for production-changing operations |
| Auditability | Record tool calls, evidence references, recommendations, and decisions |

The investigation workflow must not be able to bypass authorization controls through natural-language instructions.

Production rollbacks, infrastructure modifications, database changes, and other potentially disruptive operations remain outside autonomous execution unless separately implemented with appropriate approval and safety mechanisms.

## Expected Engineering Outcomes

This project is intended to evaluate whether Agentic AI can:

- Reduce manual effort spent collecting incident evidence.
- Improve consistency and traceability of incident investigations.
- Identify relationships across logs, metrics, traces, and deployment history.
- Help engineers distinguish symptoms from underlying causes.
- Provide technically grounded recovery recommendations.
- Support more efficient incident handoffs and post-incident reviews.
- Establish evidence for selecting appropriate LLMs for SRE workloads.

Potential improvements in Mean Time to Detect (MTTD), Mean Time to Diagnose, and Mean Time to Resolve (MTTR) must be demonstrated through controlled measurements.

**No reduction in incident resolution time is assumed before testing.**

## Implementation and Validation Roadmap

| Phase | Engineering Deliverable |
|---|---|
| 1. Microservices baseline | Operational Java microservices with repeatable workloads |
| 2. Observability | Working application logs, metrics, and distributed traces |
| 3. Controlled incidents | Repeatable failure injection with known root causes |
| 4. Agentic investigation | LangGraph workflow integrated with authorized diagnostic tools |
| 5. Root-cause validation | Evidence-based incident findings and safe recommendations |
| 6. Model comparison | Comparative evaluation of available LLMs |
| 7. AWS validation | Run the solution on AWS EKS and associated services |
| 8. Results and documentation | Publish validated observations, limitations, and evaluation results |

The implementation begins with a reproducible local environment before extending the validated scenarios to AWS.

## Project Status

**Status: Architecture and implementation planning**

The architecture, agent responsibilities, fault scenarios, and evaluation criteria described here represent the intended implementation.

Working capabilities, successful incident diagnoses, model benchmarks, and performance improvements will be documented as they are implemented and validated.

This repository does not currently claim proven MTTR improvements, autonomous production remediation, or superiority of any specific LLM.

## Intended Audience

This project is intended for Site Reliability Engineers, DevOps Engineers, Platform Engineers, Cloud Architects, AI Architects, and Engineering Leaders interested in applying Agentic AI to real-world software operations.

It emphasizes practical engineering validation rather than chatbot demonstrations or unsupported AI performance claims.

## Author

**Dr. Kishore Veleti**

Software Architecture | Enterprise AI Architecture | Cloud-Native Engineering | Agentic AI

GitHub: [javakishore-veleti](https://github.com/javakishore-veleti)

---

**AWS Agentic AI SRE Incident Intelligence**

*Building an evidence-based approach to AI-assisted incident investigation, root-cause analysis, and operational decision support.*
