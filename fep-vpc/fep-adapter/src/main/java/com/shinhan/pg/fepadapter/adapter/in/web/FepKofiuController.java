package com.shinhan.pg.fepadapter.adapter.in.web;

/** [MVP6-F03,MVP6-F05] 상세 HTTP 계약 미정.
 * TODO API: POST /internal/v1/fep/kofiu/reports (경로 후보)
 * TODO 처리: AML 자동 보고 → 서비스 인증 → 동일 reportKey 기록 → Mock KoFIU 접수 확인. 실패는 재시도 대기다.
 * TODO DTO: 요청/결과·상관 ID·단계·오류/timeout·재전송 계약 확정 후 별도 request/response 파일 작성.
 * @RestController/@Mapping은 확정 전 등록하지 않는다. 금융 FEP가 외부 LLM 요청을 중계하지 않는다.
 */
public final class FepKofiuController {
    private FepKofiuController() {}
}
