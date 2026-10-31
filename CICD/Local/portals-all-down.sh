#!/usr/bin/env bash
set -u

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "${ROOT}/../.." && pwd)"
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

for name in agentic-ai-admin ecommerce-portal; do
  pidfile="${RUN}/pids/${name}.pid"
  if [[ -f "${pidfile}" ]]; then
    stop_pid "$(cat "${pidfile}" 2>/dev/null || true)"
    rm -f "${pidfile}"
    echo "Stopped ${name}"
  fi
done

for port in 5173 5174; do
  pids="$(lsof -nP -t -iTCP:${port} -sTCP:LISTEN 2>/dev/null || true)"
  for pid in ${pids}; do
    command="$(ps -p "${pid}" -o command= 2>/dev/null || true)"
    case "${command}" in
      *"${REPO}/Portals/"*|*"vite"*)
        stop_pid "${pid}"
        echo "Stopped listener ${pid} on ${port}"
        ;;
    esac
  done
done

echo "Portals are stopped."
