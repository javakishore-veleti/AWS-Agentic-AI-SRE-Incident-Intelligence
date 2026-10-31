#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if ! docker network inspect sre-incident-net >/dev/null 2>&1; then
  docker network create sre-incident-net
fi

for stack in Postgres Redis Prometheus ELK Jaeger; do
  echo "Starting ${stack}"
  docker compose -f "${ROOT}/${stack}/docker-compose.yaml" up -d
done

echo
echo "Local containers are up."
echo "  Postgres        localhost:5432"
echo "  Redis           localhost:6379"
echo "  Prometheus      http://localhost:9090"
echo "  Elasticsearch   http://localhost:9200"
echo "  Kibana          http://localhost:5601"
echo "  Logstash TCP    localhost:5044"
echo "  Jaeger UI       http://localhost:16686"
echo "  Jaeger OTLP     localhost:4318"
