package com.shinhan.pg.apigateway.config;

/** [MVP2-F13, MVP7-F06] 고객 Gateway의 운영 API는 보류한다.
 * 로컬 MVP1 검증은 사용자 요청(2026-10-19)으로 infra/docker/ops_api.py에 구현했다.
 * POST/GET /ops/v1/performance-runs 및 GET /{runId}: 별도 loopback 서버 + 운영 토큰.
 * Docker 실행 권한을 고객 Gateway에 부여하거나 고객 JWT로 검증 스택을 만들지 않는다.
 * TODO 후보: GET /ops/v1/services/overview 및 AWS 운영/실제 부하 실행 API.
 * 위 후보의 Controller·route·자체 DB는 계속 보류한다.
 */
public final class DeferredOperationsApiPlan {
    private DeferredOperationsApiPlan() {}
}
