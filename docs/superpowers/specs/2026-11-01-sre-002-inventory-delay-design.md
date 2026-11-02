# SRE-002 inventory delay

Issue #14. A reservation reads `fault.response-delay-ms` on each call. One inventory test waits at 300ms and the next reservation, after that value is removed, is faster. One orders test records an inventory client span that includes a 400ms response, then a later purchase whose inventory span is shorter. Orders gives up after 8 seconds.

The k6 profile is `CICD/Local/k6/inc-002-delay.js`. It was not executed. Jaeger was not opened.
