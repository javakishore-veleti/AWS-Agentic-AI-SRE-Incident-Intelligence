---
name: gh-actions-aws-codegen
description: Generate AWS CloudFormation infrastructure and manual GitHub Actions setup/destroy/deploy workflows for this SRE project.
---

# gh-actions-aws-codegen

## Objective
Generate reproducible AWS infrastructure, deployment and teardown for the five Spring Boot services, two React portals and Python LangGraph investigation service. Inspect existing repository files before editing; implement in small reviewable increments. No AWS changes without explicit execution approval.

## Infrastructure architecture
- CloudFormation templates under `CICD/AWS/CloudFormation/`: `001-network.yaml`, `002-security.yaml`, `003-ecr.yaml`, `004-rds.yaml`, `005-redis.yaml`, `006-eks.yaml`, `007-observability.yaml`, `008-ingress.yaml`. Use parameters/outputs, stack tags `Project`, `Environment`, `ManagedBy`, `Owner`, and explicit dependencies. Validate templates with `aws cloudformation validate-template` and cfn-lint where available.
- Network supports `vpc_mode=existing|new` (default existing), reading `AWS_VPC_ID` from repo/environment variable or secret. For existing VPC, inspect CIDRs, subnet AZ distribution, routing and tags; reuse existing suitable subnets or create only explicitly approved nonoverlapping project-owned subnets. Never delete reused infrastructure.
- New VPC example CIDR `10.40.0.0/16`, across two AZs: public `10.40.0.0/24`, `10.40.1.0/24`; private app `10.40.10.0/24`, `10.40.11.0/24`; private data `10.40.20.0/24`, `10.40.21.0/24`. Parameterize all CIDRs; do not assume they are available. IGW/public routing, NAT single for low-cost dev or one per AZ for resilience; private app default route through NAT or endpoints; data subnets have no direct internet route.
- EKS nodes in private app subnets; ALB in public subnets; RDS PostgreSQL and ElastiCache Redis in private data subnet groups. ECR repositories for all images. Use scoped IAM, KMS/encryption, security groups, private database endpoints and secrets management. Never expose database ports publicly.
- Use AWS-managed telemetry or self-hosted observability explicitly selected by environment. Avoid duplicate ownership of ALB: choose AWS Load Balancer Controller via Kubernetes Ingress OR CloudFormation-managed ALB, not both.

## Workflow naming and orchestration
Create `.github/workflows/AWS-001-Setup-Network.yml` and `AWS-001-Destroy-Network.yml`, through `AWS-008-Setup-Ingress.yml` / `AWS-008-Destroy-Ingress.yml`. Each component supports `workflow_dispatch` and `workflow_call` with typed inputs. Create `AWS-900-Setup-All.yml` chaining 001→008 via `jobs.<id>.uses` and `needs`, and `AWS-901-Destroy-All.yml` chaining 008→001 in reverse. Ensure actual filenames use exact numeric/aspect naming. Do not confuse issue IDs with workflow filenames.
- App workflows: `AWS-100-Deploy-Backend.yml`, `AWS-101-Deploy-Portals.yml`, `AWS-102-Deploy-Investigation-Agent.yml`, `AWS-103-Run-Smoke-Tests.yml`, `AWS-910-Deploy-All.yml`, `AWS-911-Undeploy-All.yml`. Build images and push to ECR; deploy to EKS with Helm or Kubernetes manifests, not CloudFormation-managed Pods. Ensure undeploy precedes infrastructure destroy.
- All workflows manually triggerable. Reusable workflow calls use `workflow_call`, not shell invocations of YAML. Use GitHub Environments, concurrency groups, least-privilege permissions, pinned actions and explicit `needs` dependencies.
- AWS auth: prefer OIDC and environment-scoped role `AWS_ROLE_ARN`; if user elects existing secrets, use `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, optional `AWS_SESSION_TOKEN`, and never print them. Read `AWS_REGION`, `AWS_ACCOUNT_ID`, `AWS_VPC_ID` and subnet IDs from repo variables/secrets as configured. Validate identity with STS and fail on wrong account/region.
- Destruction requires typed `DESTROY-<environment>` confirmation, approval where supported, dry-run inventory, stack ownership checks, protected data retention/snapshot policy, and outputs of deleted/retained resources. Never delete reused VPC, subnets or unrelated stacks. Ensure Kubernetes-created load balancers and volumes are cleaned before deleting EKS/network; ECR/RDS retention is explicit.
- Track setup/destroy with CloudFormation stack IDs, status, outputs, change sets and workflow summaries. Add AWS Budgets alerts and optional cost-saving configurations.

## Definition of done
Unit/static validation of templates, action YAML parse, manual workflow smoke-test plan, idempotent setup, safe repeat execution, reverse teardown, zero accidental deletion of pre-existing resources. Produce a file-by-file change summary, security review, costs and test evidence. Never claim deployment without a real run.
