# SRE-001 pool exhaustion

Issue #13. A reduced Hikari pool fails a product read, shows a timeout metric and a slow span, then recovers when the connection is released.

The k6 profile is `CICD/Local/k6/inc-001-pool.js`. It was not executed. The test is the evidence.
