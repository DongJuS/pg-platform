package com.shinhan.pg.fepadapter.application.port.in;

import com.shinhan.pg.fepadapter.application.command.ExchangeSettlementFileCommand;
import com.shinhan.pg.fepadapter.application.result.ExchangeSettlementFileResult;

/**
 * <pre>
 * [MVP7-F03] 정산 파일 송수신
 * API 초안: POST /internal/v1/fep/settlement/files
 * 상태: 신규 — skeleton profile의 유효 요청은 업무 미구현 501
 * 인증/접근: 서비스 인증 / INTERNAL
 * 입력 근거: 정산 파일
 * 출력 근거: 결과 파일
 * 정상 응답 후보: 202 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 무응답 시 재전송(같은 날짜 1회만 반영)
 * TODO: 같은 날짜가 아니라 cycle/version/fileKey를 중복 효과 식별자로 사용한다. 실제 고정길이 파일 필드는 미정이다.
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
 * </pre>
 */
public interface ExchangeSettlementFileUseCase {
    ExchangeSettlementFileResult exchangeSettlementFile(ExchangeSettlementFileCommand command);
}
