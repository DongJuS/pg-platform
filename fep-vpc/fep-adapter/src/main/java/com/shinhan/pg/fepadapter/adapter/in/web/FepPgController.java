package com.shinhan.pg.fepadapter.adapter.in.web;

/** [MVP2-F01,MVP2-F03,MVP2-F04] 상세 HTTP 계약 미정.
 * TODO API: POST /internal/v1/fep/pg/approvals, cancellations, refunds (경로 후보)
 * TODO 처리: Payment에서 서비스 인증으로 요청 → 접수 기록 → Mock PG 송수신. 최종 승인과 접수를 구분한다.
 * TODO DTO: 요청/결과·상관 ID·단계·오류/timeout·재전송 계약 확정 후 별도 request/response 파일 작성.
 * @RestController/@Mapping은 확정 전 등록하지 않는다. 금융 FEP가 외부 LLM 요청을 중계하지 않는다.
 */
public final class FepPgController {
    private FepPgController() {}
}
