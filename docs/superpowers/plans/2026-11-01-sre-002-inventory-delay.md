# SRE-002 inventory delay

1. Read `fault.response-delay-ms` on each reservation.
2. Prove a 300ms delay and the following reservation after the value is removed.
3. Prove an orders inventory span includes a 400ms response and the next purchase span is shorter.
4. Document the 8 second orders read timeout and the k6 profile. Do not claim the script or Jaeger ran.
