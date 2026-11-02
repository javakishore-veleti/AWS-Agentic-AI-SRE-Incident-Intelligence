# Setup and destroy workflows

The sixteen workflows under `.github/workflows` are `AWS-001` through `AWS-008`, each with a setup file and a destroy file. They start only from `workflow_dispatch` or `workflow_call`. A pull request does not start them.

`dry_run` defaults to true. A dry run creates a change set or prints stack resources and does not apply the change. Destroy also requires the confirmation `DESTROY-<environment>` and refuses a stack whose tags are not `Project=Heddle` and `ManagedBy=cloudformation`.

The network destroy prints `StackOwnsVpc` and does not delete a reused VPC. Registry stacks keep their images. The database stack keeps a snapshot. The key and the generated secret are retained by the security template.

GitHub environment variables used when someone chooses to run a workflow: `AWS_ROLE_ARN`, `AWS_REGION`, `AWS_ACCOUNT_ID`, and, for an existing VPC, `AWS_VPC_ID` plus the six subnet ids. No access key is written into the workflow.

These files were checked by parsing them. They were not dispatched, and no AWS account was used.
