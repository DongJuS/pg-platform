package com.shinhan.pg.ledger.application.service;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.ledger.application.command.ApplyInstitutionResultCommand;
import com.shinhan.pg.ledger.application.command.GetBalanceCommand;
import com.shinhan.pg.ledger.application.command.ListJournalEntriesCommand;
import com.shinhan.pg.ledger.application.command.PostJournalCommand;
import com.shinhan.pg.ledger.application.command.QueryJournalCommand;
import com.shinhan.pg.ledger.application.port.in.ApplyInstitutionResultUseCase;
import com.shinhan.pg.ledger.application.port.in.GetBalanceUseCase;
import com.shinhan.pg.ledger.application.port.in.ListJournalEntriesUseCase;
import com.shinhan.pg.ledger.application.port.in.PostJournalUseCase;
import com.shinhan.pg.ledger.application.port.in.QueryJournalUseCase;
import com.shinhan.pg.ledger.application.port.out.JournalRepository;
import com.shinhan.pg.ledger.application.port.out.LedgerAccountRepository;
import com.shinhan.pg.ledger.application.port.out.LedgerAtomicWritePort;
import com.shinhan.pg.ledger.application.port.out.PostingRepository;
import com.shinhan.pg.ledger.application.result.GetBalanceResult;
import com.shinhan.pg.ledger.application.result.ListJournalEntriesResult;
import com.shinhan.pg.ledger.application.result.PostJournalResult;
import com.shinhan.pg.ledger.application.result.QueryJournalResult;

/** 기능별 계약과 TODO를 모은 application 구현 자리.
 * Controller는 이 클래스가 구현하는 input port만 호출한다. 외부 호출을 열린 금융 DB 트랜잭션 안에서 수행하지 않는다.
 * 현재 모든 업무 메서드는 명시적 미구현 예외를 던지며 성공 DTO·가짜 금융 결과를 반환하지 않는다.
 */
public final class LedgerApplicationService
        implements PostJournalUseCase,
                ApplyInstitutionResultUseCase,
                QueryJournalUseCase,
                GetBalanceUseCase,
                ListJournalEntriesUseCase {
    private final LedgerAtomicWritePort ledgerAtomicWritePort;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final JournalRepository journalRepository;
    private final PostingRepository postingRepository;

    public LedgerApplicationService(
            LedgerAtomicWritePort ledgerAtomicWritePort,
            LedgerAccountRepository ledgerAccountRepository,
            JournalRepository journalRepository,
            PostingRepository postingRepository) {
        this.ledgerAtomicWritePort = java.util.Objects.requireNonNull(ledgerAtomicWritePort);
        this.ledgerAccountRepository = java.util.Objects.requireNonNull(ledgerAccountRepository);
        this.journalRepository = java.util.Objects.requireNonNull(journalRepository);
        this.postingRepository = java.util.Objects.requireNonNull(postingRepository);
    }

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
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 한 트랜잭션, 합계 0 검증, transactionId unique로 중복 차단
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO: ledger.posted만 원장 Outbox에 저장한다. transfer.completed는 Transfer의 책임이다.
     * TODO: 계좌 ID 정렬 잠금 → 잔액/통화별 합계 0 검증 → 분개·잔액·Outbox 같은 MariaDB 트랜잭션. 외부 HTTP를 잠금 중 호출하지 않는다.
     * TODO: 원장 동일 transactionId 결과 재조회와 클라이언트 중복 이체 409는 서로 다른 계약이다. 409/기존 journal 응답의 최종 형식은 미정이다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a498164b8eeca1655e74d32
     * </pre>
     */
    @Override
    public PostJournalResult postJournal(PostJournalCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 한 트랜잭션, 합계 0 검증, transactionId unique로 중복 차단
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: ledger.posted만 같은 원장 DB Outbox에 기록한다.
        throw new FeatureNotImplementedException("MVP1-F13");
    }

    /**
     * <pre>
     * [MVP5-F06,MVP5-F08] Kafka fep.ack 수신. 확정/역분개는 기존 거래 ID에 대해 중복 없이 처리한다.
     * TODO: ACSC → 확정 분개, RJCT → 역분개로 고객 계좌 전액 반환 / 예외: 중복 응답은 무시
     * TODO: fep.ack 발행자·소비자와 최종 상태 갱신 책임을 정함. 수신 확인·진행 중·최종 결과를 구분하고 확정·역분개는 멱등 처리. / 예외: 미확정 응답·계속 무응답은 확인 대기. 지원 상태 코드와 최종성 정의는 보류.
     * TODO: 재시도·중복 효과 방지·로컬 트랜잭션·상태/Outbox를 연결한다. 실제 DB/통신 구현 없음.
     * </pre>
     */
    @Override
    public void applyInstitutionResult(ApplyInstitutionResultCommand command) {
        // TODO: 위 처리 순서를 구현하고 DB/외부 시스템 검증을 추가한다.
        throw new FeatureNotImplementedException("MVP5-F06");
    }

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
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 재시도·대사용
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO: 조회 404만으로 이체의 최종 실패를 단정하지 않는다. 처리 중/불명과 미반영의 구분은 후속 계약이다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981f79aa8dadf63136167
     * </pre>
     */
    @Override
    public QueryJournalResult queryJournal(QueryJournalCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 재시도·대사용
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP1-F14");
    }

    /**
     * <pre>
     * [MVP1-F11] 잔액 조회
     * API 초안: GET /api/v1/accounts/{accountId}/balances
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: accountId
     * 출력 근거: 잔액, 기준 시각
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 403
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 소유권 검사, posting 합으로 잔액 계산
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981b9b6c9fc7a3de22119
     * </pre>
     */
    @Override
    public GetBalanceResult getBalance(GetBalanceCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 소유권 검사, posting 합으로 잔액 계산
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP1-F11");
    }

    /**
     * <pre>
     * [MVP1-F12] 원장 거래내역
     * API 초안: GET /api/v1/accounts/{accountId}/entries
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: from, to, cursor
     * 출력 근거: entry 목록, nextCursor
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 403
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 소유권 검사, 시간 역순
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981ddbaffdc6c0103bf25
     * </pre>
     */
    @Override
    public ListJournalEntriesResult listJournalEntries(ListJournalEntriesCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 소유권 검사, 시간 역순
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP1-F12");
    }
}
