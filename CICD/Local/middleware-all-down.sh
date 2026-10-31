#!/usr/bin/env bash
set -u

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN="${ROOT}/run"

stop_pid() {
  local pid="$1"
  if [[ -n "${pid}" ]] && kill -0 "${pid}" >/dev/null 2>&1; then
    kill "${pid}" >/dev/null 2>&1 || true
    local child
    for child in $(pgrep -P "${pid}" 2>/dev/null || true); do
      kill "${child}" >/dev/null 2>&1 || true
    done
  fi
}

if [[ -d "${RUN}/pids" ]]; then
  for pidfile in "${RUN}/pids"/*; do
    [[ -f "${pidfile}" ]] || continue
    name="$(basename "${pidfile}" .pid)"
    pid="$(cat "${pidfile}" 2>/dev/null || true)"
    stop_pid "${pid}"
    rm -f "${pidfile}"
    echo "Stopped ${name}"
  done
fi

# Stop listeners that were launched from this repo's run jars or the investigation agent.
for port in 8081 8082 8083 8084 8085 8090; do
  pids="$(lsof -nP -t -iTCP:${port} -sTCP:LISTEN 2>/dev/null || true)"
  for pid in ${pids}; do
    command="$(ps -p "${pid}" -o command= 2>/dev/null || true)"
    case "${command}" in
      *"${RUN}/jars/"*|*"Middleware/investigation-agent"*|*"uvicorn app.main:app"*)
        stop_pid "${pid}"
        echo "Stopped listener ${pid} on ${port}"
        ;;
    esac
  done
done

echo "Middleware services are stopped."
