# AWS-001 network

The network template defaults to an existing VPC. `ExistingVpcId` receives `AWS_VPC_ID`. That VPC is not a stack resource.

`VpcMode=new` creates a project-owned VPC. CIDRs are parameters. The examples are `10.40.0.0/16`, public `10.40.0.0/24` and `10.40.1.0/24`, private app `10.40.10.0/24` and `10.40.11.0/24`, and private data `10.40.20.0/24` and `10.40.21.0/24`.

One NAT is the default. Data subnets have no internet route. Later stacks receive subnet ids as parameters, not exports.

No AWS account is used to check this template.
