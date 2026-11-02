# Security stack

`CICD/AWS/CloudFormation/002-security.yaml` creates the application, load balancer, and data security groups, the EKS roles, a rotated KMS key, and a Secrets Manager secret.

The database password is generated. It is not a template parameter and it is not a stack output. The deploy role can push images and update `heddle-<environment>-*` stacks. It cannot call `secretsmanager:GetSecretValue`.

PostgreSQL and Redis ports accept traffic only from the application security group. The data group has no public ingress.

The GitHub OIDC provider is not created unless `CreateOidcProvider` is `true`. Pass an existing provider ARN instead when the account already has one. Do not set both.

The key and the secret are retained when the stack is deleted. Roles and security groups are deleted with the stack. No AWS account was used to check this file.
