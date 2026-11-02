# SRE-001 plan

1. Add a catalog test with a one-connection pool and a 400ms acquire timeout.
2. Prove the failed read, the Hikari timeout metric, a span longer than 300ms, and the successful read after release.
3. Add the k6 profile and the setup and recovery notes. Do not claim the script ran.
