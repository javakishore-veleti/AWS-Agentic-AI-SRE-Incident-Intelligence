#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "${ROOT}/../.." && pwd)"
RUN="${ROOT}/run"
mkdir -p "${RUN}/logs" "${RUN}/pids"

if ! command -v npm >/dev/null 2>&1; then
  echo "npm was not found. Install Node.js and put npm on PATH." >&2
  exit 1
fi

PORTALS=(
  "agentic-ai-admin:5173"
  "ecommerce-portal:5174"
)

listening() {
  curl -fsS --max-time 2 "http://127.0.0.1:${1}/" >/dev/null 2>&1
}

for entry in "${PORTALS[@]}"; do
  IFS=':' read -r name port <<<"${entry}"
  if listening "${port}"; then
    echo "${name} is already up on ${port}"
    continue
  fi
  portal_dir="${REPO}/Portals/${name}"
  if [[ ! -d "${portal_dir}" ]]; then
    echo "Missing ${portal_dir}" >&2
    exit 1
  fi
  if [[ ! -d "${portal_dir}/node_modules" ]]; then
    echo "Installing ${name} dependencies"
    npm install --prefix "${portal_dir}"
  fi
  (
    cd "${portal_dir}"
    nohup npm run dev -- --host 127.0.0.1 --port "${port}" --strictPort >"${RUN}/logs/${name}.log" 2>&1 &
    echo $! >"${RUN}/pids/${name}.pid"
    echo "Started ${name} on ${port} (pid $!)"
  )
done

echo
failed=0
for entry in "${PORTALS[@]}"; do
  IFS=':' read -r name port <<<"${entry}"
  ready=0
  for _ in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do
    if listening "${port}"; then
      ready=1
      break
    fi
    sleep 1
  done
  if [[ "${ready}" -eq 1 ]]; then
    echo "  ${name}  http://127.0.0.1:${port}  up"
  else
    echo "  ${name}  http://127.0.0.1:${port}  not ready (see ${RUN}/logs/${name}.log)" >&2
    failed=1
  fi
done

if [[ "${failed}" -ne 0 ]]; then
  exit 1
fi
echo "Portals are up."
