package com.shinhan.pg.wealthreport.application.port.in;

import com.shinhan.pg.wealthreport.application.command.RetryReportCommand;
import com.shinhan.pg.wealthreport.application.result.RetryReportResult;

/**
 * <pre>
 * [MVP4-F04] 보고서 재처리
 * API 초안: POST /internal/v1/wealth/reports/{reportId}/retry
 * 상태: 보류 — Controller bean/HTTP mapping 등록 금지
 * 인증/접근: 운영자 권한 / ADMIN
 * 입력 근거: reportId
 * 출력 근거: 상태
 * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 409
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a4981529ed5d83553522998
 * </pre>
 */
public interface RetryReportUseCase {
    RetryReportResult retryReport(RetryReportCommand command);
}
