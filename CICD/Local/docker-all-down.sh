#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

for stack in Jaeger ELK Prometheus Redis Postgres; do
  echo "Stopping ${stack}"
  docker compose -f "${ROOT}/${stack}/docker-compose.yaml" down
done

docker network rm sre-incident-net >/dev/null 2>&1 || true
echo "Local containers are stopped."
