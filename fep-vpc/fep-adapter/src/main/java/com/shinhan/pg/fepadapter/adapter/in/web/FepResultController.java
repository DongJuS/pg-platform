package com.shinhan.pg.fepadapter.adapter.in.web;

/** [MVP5-F04,MVP7-F03] 상세 HTTP 계약 미정.
 * TODO API: GET /internal/v1/fep/results, POST /internal/v1/fep/callbacks (경로 후보)
 * TODO 처리: channelCode/messageKey 조회와 기관 결과 수신. QueryInstitutionResult/ReceiveInstitutionResultUseCase에 연결할 인증·최종성·payload를 확정한다.
 * TODO DTO: 요청/결과·상관 ID·단계·오류/timeout·재전송 계약 확정 후 별도 request/response 파일 작성.
 * @RestController/@Mapping은 확정 전 등록하지 않는다. 금융 FEP가 외부 LLM 요청을 중계하지 않는다.
 */
public final class FepResultController {
    private FepResultController() {}
}
