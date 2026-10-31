#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "${ROOT}/../.." && pwd)"
RUN="${ROOT}/run"
mkdir -p "${RUN}/logs" "${RUN}/pids" "${RUN}/jars"

if [[ -z "${JAVA_HOME:-}" && -x /usr/libexec/java_home ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
  export JAVA_HOME
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="${JAVA_HOME}/bin:${PATH}"
fi
if ! command -v java >/dev/null 2>&1; then
  echo "Java 21 was not found. Set JAVA_HOME or install a JDK." >&2
  exit 1
fi

if ! command -v mvn >/dev/null 2>&1; then
  for candidate in \
    "${HOME}/kishore/software/maven/apache-maven-3.9.11/bin" \
    /opt/homebrew/bin \
    /usr/local/bin
  do
    if [[ -x "${candidate}/mvn" ]]; then
      export PATH="${candidate}:${PATH}"
      break
    fi
  done
fi
if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven was not found. Install Maven or put it on PATH." >&2
  exit 1
fi

SERVICES=(
  "product-catalog-service:8081:/actuator/health"
  "inventory-service:8085:/actuator/health"
  "offers-service:8082:/actuator/health"
  "customers-service:8083:/actuator/health"
  "orders-service:8084:/actuator/health"
)

healthy() {
  local port="$1"
  local path="$2"
  curl -fsS --max-time 2 "http://127.0.0.1:${port}${path}" >/dev/null 2>&1
}

needs_build=0
for entry in "${SERVICES[@]}"; do
  IFS=':' read -r name port path <<<"${entry}"
  if ! healthy "${port}" "${path}"; then
    needs_build=1
  fi
done

if [[ "${needs_build}" -eq 1 ]]; then
  echo "Building Middleware"
  (cd "${REPO}/Middleware" && mvn -DskipTests package -q)
fi

for entry in "${SERVICES[@]}"; do
  IFS=':' read -r name port path <<<"${entry}"
  if healthy "${port}" "${path}"; then
    echo "${name} is already up on ${port}"
    continue
  fi
  jar_source="$(find "${REPO}/Middleware/${name}/target" -maxdepth 1 -type f -name "${name}-*.jar" ! -name '*.original' | head -1)"
  if [[ -z "${jar_source}" ]]; then
    echo "No jar found for ${name}." >&2
    exit 1
  fi
  cp "${jar_source}" "${RUN}/jars/${name}.jar"
  nohup java -jar "${RUN}/jars/${name}.jar" >"${RUN}/logs/${name}.log" 2>&1 &
  echo $! >"${RUN}/pids/${name}.pid"
  echo "Started ${name} on ${port} (pid $!)"
done

if command -v python3 >/dev/null 2>&1 && healthy 8090 /health; then
  echo "investigation-agent is already up on 8090"
elif command -v python3 >/dev/null 2>&1 && python3 -c "import uvicorn, fastapi, langgraph" >/dev/null 2>&1; then
  (
    cd "${REPO}/Middleware/investigation-agent"
    nohup python3 -m uvicorn app.main:app --host 127.0.0.1 --port 8090 >"${RUN}/logs/investigation-agent.log" 2>&1 &
    echo $! >"${RUN}/pids/investigation-agent.pid"
    echo "Started investigation-agent on 8090 (pid $!)"
  )
else
  echo "investigation-agent was not started. Install Middleware/investigation-agent/requirements.txt to run it on 8090."
fi

echo
echo "Waiting for health checks"
failed=0
for entry in "${SERVICES[@]}"; do
  IFS=':' read -r name port path <<<"${entry}"
  ready=0
  for _ in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20; do
    if healthy "${port}" "${path}"; then
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
echo "Middleware services are up."
