#!/usr/bin/env bash
# load-test/run.sh — k6 Docker 래퍼
#
# 사용:
#   ./run.sh <script.js>              # 예: ./run.sh baseline-health.js
#   VAR=value ./run.sh <script.js>    # 환경변수는 그대로 k6 컨테이너에 -e로 전달
#
# 네트워크:
#   기본(host 모드)   : k6 컨테이너가 host.docker.internal:380xx(dev-access
#                        forwarder 경유 호스트 포트)로 접근.
#   TARGET_MODE=compose: compose 프로젝트 네트워크(sinhan_project_docker_main)에 붙어
#                        서비스 이름(transfer:8084 등)으로 접근.
#                        main은 internal: true이지만 컨테이너 attach는 가능하다
#                        (internal은 외부 egress만 차단). k6는 스크립트를 bind
#                        mount하고 이미지는 사전 pull하므로 외부 통신이 필요 없다.
#
# 결과: 스크립트가 handleSummary로 results/*-summary.json에 기록한다.
#       (k6 컨테이너의 /scripts 아래에 마운트되므로 호스트 load-test/results/에 남는다)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

if [[ $# -lt 1 ]]; then
  echo "usage: $0 <k6-script.js> [extra k6 args...]" >&2
  exit 2
fi
SCRIPT="$1"; shift

K6_IMAGE="${K6_IMAGE:-grafana/k6:latest}"
COMPOSE_NETWORK="${COMPOSE_NETWORK:-sinhan_project_docker_main}"

DOCKER_ARGS=(--rm -i
  -w /scripts
  -v "${SCRIPT_DIR}/k6:/scripts"
  -v "${SCRIPT_DIR}/results:/scripts/results")

if [[ "${TARGET_MODE:-host}" == "compose" ]]; then
  DOCKER_ARGS+=(--network "${COMPOSE_NETWORK}")
fi

# 호스트의 TARGET_*·SERVICES·*_BASE_URL 등 k6 관련 환경변수를 컨테이너로 전달
while IFS='=' read -r name _; do
  case "${name}" in
    TARGET_*|SERVICES|RUN_ID|VARIANT|RPS|BASE_RPS|DURATION|AMOUNT| \
    ACCOUNT_POOL|CONTENTION_ACCOUNT_ID|PAYMENT_COUNT|REFUND_RPS| \
    SETTLEMENT_DATE|RERUN_RPS|*_BASE_URL| \
    HEALTH_*|BIZ_*|REPORT_BUDGET_MS|REQ_TIMEOUT)
      DOCKER_ARGS+=(-e "${name}=${!name}") ;;
  esac
done < <(env)

docker run "${DOCKER_ARGS[@]}" "${K6_IMAGE}" run "/scripts/${SCRIPT}" "$@"
