# AWS-002 security

Security groups, a rotated key, and a generated database secret live in one template. The secret value is not an output. Database ports accept traffic only from the application security group.

The GitHub deploy role is optional. It is created only when an OIDC provider is created or an existing provider ARN is passed. Its resource scope is the `heddle-<environment>-*` names.

No AWS account is used to check this template.
