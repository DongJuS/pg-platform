package com.shinhan.pg.aml.application.port.in;

import com.shinhan.pg.aml.application.command.SubmitAmlReportCommand;
import com.shinhan.pg.aml.application.result.SubmitAmlReportResult;

/**
 * <pre>
 * [MVP6-F03] KoFIU 보고 전송
 * API 초안: POST /internal/v1/aml/cases/{caseId}/reports
 * 상태: 신규 — skeleton profile의 유효 요청은 업무 미구현 501
 * 인증/접근: 운영자 권한 / ADMIN
 * 입력 근거: caseId
 * 출력 근거: 200이면 '보고 완료'
 * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 오류 코드면 '전송 실패', 재전송 가능
 * TODO: 사람 승인 단계를 추가하지 않는다. 자동 판정한 보고서의 생성·전송 유스케이스 진입점이다.
 * TODO: FEP 접수 성공과 KoFIU Mock 최종 접수를 구분한다. 전송 실패는 보고 확정 취소가 아니라 재전송 대상이다.
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
 * </pre>
 */
public interface SubmitAmlReportUseCase {
    SubmitAmlReportResult submitAmlReport(SubmitAmlReportCommand command);
}
