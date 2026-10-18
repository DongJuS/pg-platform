package com.shinhan.pg.aml.application.port.in;

import com.shinhan.pg.aml.application.command.ListAmlCasesCommand;
import com.shinhan.pg.aml.application.result.ListAmlCasesResult;

/**
 * <pre>
 * [MVP6-F02] 케이스 목록 조회
 * API 초안: GET /internal/v1/aml/cases
 * 상태: 신규 — skeleton profile의 유효 요청은 업무 미구현 501
 * 인증/접근: 운영자 권한 / ADMIN
 * 입력 근거: 상태, 기간
 * 출력 근거: 케이스 목록
 * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: —
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
 * </pre>
 */
public interface ListAmlCasesUseCase {
    ListAmlCasesResult listAmlCases(ListAmlCasesCommand command);
}
