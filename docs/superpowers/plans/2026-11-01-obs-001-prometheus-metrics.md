# OBS-001 plan

1. Publish an HTTP request histogram from each commerce service.
2. Read `/actuator/prometheus` in a test for catalog, offers, customers, orders, and inventory.
3. Require the latency bucket, a 404 status count, and the HikariCP connection series.
4. Record those PromQL series in ARCHITECTURE.
