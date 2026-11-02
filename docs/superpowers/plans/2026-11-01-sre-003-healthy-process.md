# SRE-003 healthy process

1. Read `fault.fail-requests` on each catalog request.
2. Fail `/api/**` with 500 while `/actuator/health` stays up.
3. Prove the product read succeeds after the value is removed.
4. Document the k6 profile. Do not claim the script ran.
