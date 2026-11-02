The backend manifest uses NodePort. It does not create a load balancer. The database password is read from a secret at deploy time and is not stored in the manifest. AWS-100 was not dispatched.
