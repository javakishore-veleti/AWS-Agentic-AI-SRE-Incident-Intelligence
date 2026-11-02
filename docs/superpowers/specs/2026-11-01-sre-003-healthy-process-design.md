# SRE-003 healthy process

Issue #15. Catalog reads `fault.fail-requests` on each request. When it is true, `/api/**` returns 500 and `/actuator/health` stays up. The next product read succeeds after the value is removed. A health check during the failure does not show it.

The k6 profile is `CICD/Local/k6/inc-003-healthy-process.js`. It was not executed.
