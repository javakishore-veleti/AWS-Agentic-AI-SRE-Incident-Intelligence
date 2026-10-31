#!/usr/bin/env bash
set -u

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if docker network inspect sre-incident-net >/dev/null 2>&1; then
  echo "Network sre-incident-net: present"
else
  echo "Network sre-incident-net: absent"
fi

for stack in Postgres Redis Prometheus ELK; do
  echo
  echo "== ${stack} =="
  docker compose -f "${ROOT}/${stack}/docker-compose.yaml" ps || true
done
