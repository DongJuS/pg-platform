package com.shinhan.pg.ledger.application.port.in;

import com.shinhan.pg.ledger.application.command.PostJournalCommand;
import com.shinhan.pg.ledger.application.result.PostJournalResult;

/**
 * <pre>
 * [MVP1-F13] 분개 반영
 * API 초안: POST /internal/v1/ledger/postings
 * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
 * 인증/접근: 서비스 인증 / INTERNAL
 * 입력 근거: transactionId, posting 목록
 * 출력 근거: journalId
 * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 409 중복(기존 journal 반환), 422 합계≠0
 * TODO: ledger.posted만 원장 Outbox에 저장한다. transfer.completed는 Transfer의 책임이다.
 * TODO: 계좌 ID 정렬 잠금 → 잔액/통화별 합계 0 검증 → 분개·잔액·Outbox 같은 MariaDB 트랜잭션. 외부 HTTP를 잠금 중 호출하지 않는다.
 * TODO: 원장 동일 transactionId 결과 재조회와 클라이언트 중복 이체 409는 서로 다른 계약이다. 409/기존 journal 응답의 최종 형식은 미정이다.
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a498164b8eeca1655e74d32
 * </pre>
 */
public interface PostJournalUseCase {
    PostJournalResult postJournal(PostJournalCommand command);
}
