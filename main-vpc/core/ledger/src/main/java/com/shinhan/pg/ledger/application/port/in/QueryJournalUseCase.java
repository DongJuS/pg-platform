package com.shinhan.pg.ledger.application.port.in;

import com.shinhan.pg.ledger.application.command.QueryJournalCommand;
import com.shinhan.pg.ledger.application.result.QueryJournalResult;

/**
 * <pre>
 * [MVP1-F14] 반영 결과 조회
 * API 초안: GET /internal/v1/ledger/transactions/{transactionId}
 * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
 * 인증/접근: 서비스 인증 / INTERNAL
 * 입력 근거: transactionId
 * 출력 근거: journal 또는 404
 * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 404 미반영
 * TODO: 조회 404만으로 이체의 최종 실패를 단정하지 않는다. 처리 중/불명과 미반영의 구분은 후속 계약이다.
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a4981f79aa8dadf63136167
 * </pre>
 */
public interface QueryJournalUseCase {
    QueryJournalResult queryJournal(QueryJournalCommand command);
}
