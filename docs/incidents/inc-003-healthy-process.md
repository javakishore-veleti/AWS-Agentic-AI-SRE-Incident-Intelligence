# INC-003 application failure with a healthy process

Set `FAULT_FAIL_REQUESTS=true` on Catalog Service and restart it. Requests under `/api/` return 500. `/actuator/health` stays up.

A health check does not show this failure. Read a product list, or the access log for that 500, to see it. Set `FAULT_FAIL_REQUESTS` back to `false`, restart Catalog Service, and read the product list again. That read succeeds while health was up the whole time.

`HealthyProcessTest` is the evidence. `CICD/Local/k6/inc-003-healthy-process.js` is the load profile. It was not run.
