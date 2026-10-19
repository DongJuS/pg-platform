// load-test/k6/lib/config.js
// 공통 설정: 로컬 Docker 스택 ↔ AWS 전환 시 환경변수만 바꾸면 된다.
//
// TARGET_MODE:
//   host    (기본) k6 컨테이너에서 host.docker.internal + 호스트 포트로 접근
//   compose        compose 프로젝트 네트워크(sinhan_project_docker_main) 안에서
//                  서비스 이름 + 내부 포트로 접근 (run.sh --network 사용)
//
// 개별 서비스 오버라이드: TRANSFER_BASE_URL=http://10.0.0.1:8084 처럼
// <SERVICE>_BASE_URL 환경변수가 있으면 최우선 적용 (AWS ALB 엔드포인트 등).

const MODE = __ENV.TARGET_MODE || 'host';

// 호스트 포트: canonical 스택(infra/docker/compose.yaml)의 dev-access forwarder
// 매핑 기준 — 컨테이너 8080~8095 ↔ 호스트 38080~38095 1:1 (services.tsv 순번).
// (구 infra/docker-compose 스택의 180xx 규약은 폐기됨 — legacy 결과 파일 참조)
const HOST_PORTS = {
  'api-gateway': 38080,
  account: 38082,
  ledger: 38083,
  transfer: 38084,
  payment: 38085,
  settlement: 38086,
  'wealth-report': 38088,
};

// 컨테이너 내부 포트 (각 서비스 application.yml server.port 기준)
const CONTAINER_PORTS = {
  'api-gateway': 8080,
  account: 8082,
  ledger: 8083,
  transfer: 8084,
  payment: 8085,
  settlement: 8086,
  'wealth-report': 8088,
};

export function baseUrl(service) {
  const envKey = service.toUpperCase().replace(/-/g, '_') + '_BASE_URL';
  if (__ENV[envKey]) return __ENV[envKey];
  if (MODE === 'compose') {
    return `http://${service}:${CONTAINER_PORTS[service]}`;
  }
  const host = __ENV.TARGET_HOST || 'host.docker.internal';
  return `http://${host}:${HOST_PORTS[service]}`;
}

// --- 임계값 (환경변수로 조정) ------------------------------------------------
function num(name, dflt) {
  const v = __ENV[name];
  return v === undefined || v === '' ? dflt : Number(v);
}

export const LIMITS = {
  // 헬스 기준선
  healthP95Ms: num('HEALTH_P95_MS', 500),
  healthP99Ms: num('HEALTH_P99_MS', 1000),
  healthErrorRate: num('HEALTH_ERROR_RATE', 0.01),
  // 업무 경로 (구현 후 튜닝; 초기값은 합리적 출발점일 뿐 확정 SLA가 아님 — 확인 필요)
  bizP95Ms: num('BIZ_P95_MS', 2000),
  bizP99Ms: num('BIZ_P99_MS', 5000),
  bizErrorRate: num('BIZ_ERROR_RATE', 0.01),
  // 보고서 동기 생성 시간 예산 (확정 결정: 동기 응답; 예산 값 자체는 확인 필요)
  reportBudgetMs: num('REPORT_BUDGET_MS', 10000),
  // 요청당 HTTP 타임아웃
  reqTimeout: __ENV.REQ_TIMEOUT || '15s',
};

// --- 공통 유틸 ---------------------------------------------------------------
// 멱등키: 실행(run) 내 유일성을 보장하는 결정적 키. 외부 UUID 라이브러리 불필요.
export function idempotencyKey(prefix) {
  const run = __ENV.RUN_ID || 'local';
  return `${prefix}-${run}-vu${__VU}-it${__ITER}`;
}

export function jsonHeaders(extra) {
  const h = { 'Content-Type': 'application/json' };
  return Object.assign(h, extra || {});
}

// 업무 경로 공통 판정: 스켈레톤 501은 "미구현"이지 성공이 아니다.
// EXPECT_NOT_IMPLEMENTED=true 이면 501을 예상 응답으로 기록만 하고
// 업무 성공 카운트에는 넣지 않는다(가짜 성공 방지).
export function classifyBusiness(res) {
  if (res.status === 501) return 'not_implemented';
  if (res.status >= 200 && res.status < 300) return 'ok';
  if (res.status === 409) return 'conflict';
  return 'error';
}
