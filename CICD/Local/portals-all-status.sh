#!/usr/bin/env bash
set -u

printf "%-22s %-6s %s\n" "PORTAL" "PORT" "STATUS"
for entry in "agentic-ai-admin|5173" "ecommerce-portal|5174"; do
  IFS='|' read -r name port <<<"${entry}"
  if curl -fsS --max-time 2 "http://127.0.0.1:${port}/" >/dev/null 2>&1; then
    status="up"
  else
    status="down"
  fi
  printf "%-22s %-6s %s\n" "${name}" "${port}" "${status}"
done
