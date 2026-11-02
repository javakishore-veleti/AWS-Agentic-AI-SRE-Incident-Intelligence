# Network stack

`CICD/AWS/CloudFormation/001-network.yaml` is the network template. A workflow that deploys it is added with the other setup workflows. This file does not deploy anything.

## Modes

`VpcMode` defaults to `existing`. Pass `AWS_VPC_ID` as `ExistingVpcId`, plus the six subnet ids. In that mode the template creates no VPC, subnet, route, or NAT. The reused VPC is not a stack resource, so deleting the stack cannot delete it.

Set `CreateProjectSubnets` to `true` only when the reused VPC needs new project-owned subnets. Those subnets belong to the stack. The VPC still does not.

`VpcMode=new` creates a VPC from `VpcCidr` (example `10.40.0.0/16`) and six subnets in the first two availability zones. Confirm the CIDRs are free before use. `StackOwnsVpc` is `true` only in this mode, and the VPC `DeletionPolicy` is `Delete`.

## Routes

Public subnets use an internet gateway. Private app subnets use one NAT gateway unless `NatMode` is `per-az`. Private data subnets have a route table with no `0.0.0.0/0` route. An S3 gateway endpoint is added only when the stack owns the subnets.

## Cost and safety

A NAT gateway is the hourly charge, and a second NAT is created only for `per-az`. Existing mode with `CreateProjectSubnets=false` creates no billable network resources. A monthly budget is created only when `VpcMode` is `new` and `BudgetEmail` is set. The default amount is 25 USD.

Do not deploy this template until an account, region, and OIDC role are chosen. The checks in `CICD/AWS/tests` parse the template and run cfn-lint. They do not call AWS.
