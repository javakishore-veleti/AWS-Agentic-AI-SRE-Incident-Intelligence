#!/usr/bin/env bash
set -u

checks=(
  "product-catalog-service|8081|/actuator/health"
  "offers-service|8082|/actuator/health"
  "customers-service|8083|/actuator/health"
  "orders-service|8084|/actuator/health"
  "inventory-service|8085|/actuator/health"
  "investigation-agent|8090|/health"
)

printf "%-28s %-6s %s\n" "SERVICE" "PORT" "STATUS"
for entry in "${checks[@]}"; do
  IFS='|' read -r name port path <<<"${entry}"
  if curl -fsS --max-time 2 "http://127.0.0.1:${port}${path}" >/dev/null 2>&1; then
    status="up"
  else
    status="down"
  fi
  printf "%-28s %-6s %s\n" "${name}" "${port}" "${status}"
done
