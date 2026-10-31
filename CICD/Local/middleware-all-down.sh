#!/usr/bin/env bash
set -u

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN="${ROOT}/run"

SERVICES=(
  "product-catalog-service:8081:/actuator/health"
  "offers-service:8082:/actuator/health"
  "customers-service:8083:/actuator/health"
  "orders-service:8084:/actuator/health"
  "inventory-service:8085:/actuator/health"
  "investigation-agent:8090:/health"
)

stop_pid() {
  local pid="$1"
  local child
  local _
  if [[ -z "${pid}" ]] || ! kill -0 "${pid}" >/dev/null 2>&1; then
    return 0
  fi
  kill "${pid}" >/dev/null 2>&1 || true
  for child in $(pgrep -P "${pid}" 2>/dev/null || true); do
    kill "${child}" >/dev/null 2>&1 || true
  done
  for _ in 1 2 3 4 5 6 7 8 9 10; do
    if ! kill -0 "${pid}" >/dev/null 2>&1; then
      return 0
    fi
    sleep 0.5
  done
  kill -9 "${pid}" >/dev/null 2>&1 || true
}

if [[ -d "${RUN}/pids" ]]; then
  for pidfile in "${RUN}/pids"/*; do
    [[ -f "${pidfile}" ]] || continue
    name="$(basename "${pidfile}" .pid)"
    case "${name}" in
      agentic-ai-admin|ecommerce-portal) continue ;;
    esac
    pid="$(cat "${pidfile}" 2>/dev/null || true)"
    stop_pid "${pid}"
    rm -f "${pidfile}"
    echo "Stopped ${name}"
  done
fi

for entry in "${SERVICES[@]}"; do
  IFS=':' read -r name port _ <<<"${entry}"
  pids="$(lsof -nP -t -iTCP:${port} -sTCP:LISTEN 2>/dev/null || true)"
  for pid in ${pids}; do
    command="$(ps -p "${pid}" -o command= 2>/dev/null || true)"
    case "${command}" in
      *"${name}"*|*"${RUN}/jars/"*|*"uvicorn app.main:app"*)
        stop_pid "${pid}"
        echo "Stopped ${name} (${pid}) on ${port}"
        ;;
      *)
        echo "${port} is in use by pid ${pid}, which is not ${name}." >&2
        ;;
    esac
  done
done

failed=0
for entry in "${SERVICES[@]}"; do
  IFS=':' read -r name port path <<<"${entry}"
  if curl -fsS --max-time 2 "http://127.0.0.1:${port}${path}" >/dev/null 2>&1; then
    echo "${name} is still up on ${port}." >&2
    failed=1
  fi
done

if [[ "${failed}" -ne 0 ]]; then
  exit 1
fi

echo "Middleware services are stopped."
