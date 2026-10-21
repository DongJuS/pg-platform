# MVP6 AML 구현·검증 체크리스트

개발용 의심거래 시뮬레이션이다. 실제 KoFIU 신고·현금 CTR·규제 준수 구현이 아니다. F06, 계좌 정지·자금 동결·송금 차단, 실제 기관·AWS·LLM 활성화는 포함하지 않는다.

## 설계 근거와 차이

2026-10-21 연결된 Chrome에서 Figma `19:2` 시스템 아키텍처 구성도, 그 안에서 실제 확인한 `19:771` **자금세탁**, `2:2` **00_전체아키텍처**를 읽었다. `19:771`은 거래 이벤트 수집 → 룰 탐지 → 위험 점수·케이스 → 자동 판정(사람 검토 없음) → JSON 보고서 → FEP 경유 KoFIU Mock 흐름이다. 하위 노드 ID는 추정하지 않았다. MCP metadata는 조회됐으나 design_context가 Starter 한도에 걸려 브라우저로 대조했다.

도식의 CTR·STR 구분, 포괄적 PostgreSQL 표기, 오류면 재전송이라는 요약은 현재 확정 계약보다 넓다. 사용자 지시로 F06은 보류하며 Account/Ledger는 MariaDB, AML/FEP는 PostgreSQL이다. 재전송은 조회에서 기관 미접수가 확인되고 명시 요청한 경우만 허용한다. `ledger.posted`는 Ledger, `transfer.completed`는 Transfer가 발행한다(ADR 0001 우선). Notion PRD·기능명세·코드 컨벤션은 보조 대조했으며 첨부 PDF/unknown 블록과 외부 AI 리뷰는 확인하지 않았다.

## 사용자 확정 정책 (2026-10-21)

- [x] 국내 이체·결제·해외송금의 **확정 금융 효과**를 수집한다. 원본 서비스의 거래 종류·고객·금액·통화와 Ledger 분개를 함께 대조한다.
- [x] 취소·환불·해외 역분개는 원거래에 연결해 보존한다. 신규 거래 횟수·금액에 더하지 않으며 확정된 보고를 취소하지 않는다. 해외 대기 분개는 명시적으로 제외한다.
- [x] 고객·통화별 발생 시각 기준으로 집계하며 환율 없이 통화를 합산하지 않는다. 단건 조건은 창 합계로 대체하지 않는다.

| development-v1 규칙 | KRW | USD | 점수 |
| --- | --- | --- | --- |
| 단건 금액 | 5,000,000 이상 | 5,000 이상 | 60 |
| 1시간 거래 횟수 | 5건 이상 | 5건 이상 | 40 |
| 24시간 총거래액 | 20,000,000 이상 | 20,000 이상 | 70 |

합계 **70점 이상** 자동 보고한다. 경계는 `[발생 시각−창, 발생 시각]` 양끝 포함이다. 같은 ruleVersion의 조건 변경은 충돌로 거절한다. 지연 사건이 영향을 주는 후속 창도 재평가한다. 이미 만든 보고 내용은 고정하고 추가 근거로 달라지면 케이스를 REVIEW로 남긴다. 이는 현금 CTR 기준이 아니다.

- [x] 개발용 `STR_SIMULATION`만 자동 생성·전송한다. 고정 reportId/reportKey와 내용 해시를 사용한다.
- [x] 최초 보고에는 사람 승인 단계가 없다. 관리자의 수동 전송 API는 이미 자동 판정된 보고를 전달하는 진입점이다.
- [x] 수정 보고 기준: 최종 접수된 원보고에 추가 근거가 생긴 REVIEW 케이스에서 관리자 명시 요청으로 별도 `STR_AMENDMENT_SIMULATION`을 생성한다. `supersedesReportId`, 증가하는 revision, `LATE_EVIDENCE` 사유를 저장한다. 원보고는 변경하지 않으며 같은 원보고의 반복 수정 요청은 같은 새 보고를 반환한다. 자동 취소·오탐 종결은 하지 않는다.
- [x] 관리자만 케이스·근거·보고 문서를 조회한다. 일반 사용자의 AML 조회 경로는 제공하지 않는다. 개발용 관리자 신원은 별도 `aml-operations` 호출자 자격증명으로 인증한다. 임의 역할 헤더·일반 JWT·다른 서비스의 자격증명은 권한을 주지 않는다.
- [x] 원문 이름·계좌번호·OTP·비밀번호를 수집하지 않는다. 내부 고객/계좌/거래 ID, 금액·통화·규칙 근거만 보관한다.
- [x] **AML 자체 정책으로 48시간 보존**한다. SWIFT 설정을 상속하지 않는다. 읽기는 만료 즉시 차단하고 정리 작업이 금융 사실·케이스·보고·감사·Outbox 내용을 삭제한다. 수정 보고 보존은 원 케이스/사실의 만료를 넘기지 않는다. FEP/Mock 보고 접수 내용도 48시간 뒤 삭제한다. 재전달 방지에는 원문 ID와 고객 정보를 담지 않는 SHA-256 tombstone만 남긴다.

## 구현 범위 / 파일 대조

| 기능 | 실제 구현 |
| --- | --- |
| F01 수집·탐지 | EvaluateTransactionConsumerSkeleton, AmlFactsRestAdapter, AmlReportingDecisionPolicy, DetectionRulePersistenceAdapter |
| F02 관리자 조회 | AmlCaseController / AmlOperationsController, AmlCasePersistenceAdapter, 상태·기간·페이지/크기 검증, 원본·최신 평가 근거 |
| F03 생성·전송 | AmlApplicationService, AmlWorkflowAdapter, AmlReportPersistenceAdapter, FepKofiuRestAdapter → FepKofiuController → KofiuMockRestAdapter → MockKofiuController |
| F04 자동 판정 | AmlDevelopmentPolicy / AmlRuleSet, 통화별 조건·점수·판정, 규칙 버전·근거·보고 내용 고정 |
| F05 중복·복구 | AmlAtomicWritePort, PostgreSQL Inbox/fact unique·고객/통화 잠금·case/report/Outbox 원자성, FEP 시도 lease·조회·명시 재전송, Mock 영속 dedup |
| F06 | DeferredCtrRulePlan 그대로 보류 |

Entity/Mapper는 SQL 투영 모델로 구현했다. 미사용 스켈레톤 `ProcessedEffectRepository`는 제거하고 이벤트 Inbox와 원본 금융 사실의 중복 제약을 분리했다. Kafka의 ack는 AML Inbox commit 뒤 수행한다. 원본 REST 장애 시 Inbox에서 PENDING을 복구한다. 원본 누락·만료·충돌을 정상 거래/0점으로 처리하지 않는다. `fep.ack`는 AML 소비 대상이 아니다.

다른 서비스의 새 계약은 **GET /internal/v1/ledger/aml-facts/{transactionId}**, **GET /internal/v1/transfers/aml-facts/{transactionId}**, **GET /internal/v1/payments/aml-facts/{transactionId}**뿐이다. 각 서비스가 자기 DB만 읽으며 `aml` 전용 토큰과 호출자 제한을 적용한다. 기존 외부 API, PG/SWIFT 요청·재전송 계약과 저장 스키마는 바꾸지 않는다.

## 관리자 API

모두 `/internal/v1/aml` 아래이며 Gateway의 기존 internal 차단을 유지한다. 내부 경계가 `X-Internal-Caller`와 그 호출자 전용 `X-Internal-Service-Token`을 함께 인증한다. 조회·제어는 `aml-operations`만 가능하다.

| 메서드 / 경로 | 입력 / 의미 |
| --- | --- |
| GET /cases | status, from(포함), to(미포함), page(0부터), size(1~100); nextPage와 생성 시각 반환 |
| GET /cases/{id} | 케이스 요약·원본/최신 평가 근거 |
| GET /cases/{id}/report | 최신 보고의 AML workflowStatus·전송 상태·기관 상태·접수 참조 |
| GET /cases/{id}/report-document | 내부 식별자·금액·근거만 담은 고정 보고 문서 |
| POST /cases/{id}/reports | 이미 확정된 보고의 최초 전달; 새 판정/사람 승인이 아님 |
| POST /cases/{id}/report-queries | FEP 경유 결과 조회; 조회 결과만 상태에 반영 |
| POST /cases/{id}/report-retransmissions | FEP의 **현재** 기관 미접수 조회 성공, 간격·횟수 통과 시 같은 보고 내용 재전송 |
| POST /cases/{id}/report-amendments | `{previousReportId}`; 원보고와 분리한 수정 보고 |
| POST /reevaluations | 현재 설정된 명시적 규칙 버전으로 보존 중인 사실 재평가 |

오류는 고정 코드/문구로 400(입력), 403(인증·권한), 404(부재·만료), 409(내용/상태 충돌·비활성 정책), 502(연동 불명), 503(DB 장애)를 구분한다. GET은 판정·보고·전송을 변경하지 않으며 접근 감사만 기록한다.

## 상태·복구·활성화

- AML 보고: READY → SENDING → FEP_RECEIVED / UNKNOWN → REPORTED / REJECTED / REVIEW. 전송 실패·불명은 판정 취소나 정상 종결이 아니다. 확정 결과와 충돌하는 결과는 REVIEW다.
- FEP: READY / SENDING / SENT / UNKNOWN / FAILED라는 전송 상태와 NOT_RECEIVED / PENDING / ACCEPTED / REJECTED / UNKNOWN / REVIEW라는 기관 상태를 분리한다. POST 202는 FEP의 영속 접수다.
- 구현 기본값: 결과 조회 간격 10초, 자동 조회 최대 12회 후 REVIEW; 전송 lease 30초; 명시 재전송은 최초 포함 최대 3회·최소 30초. 설정 가능하며 개발용 보수적 기본값이다. 자동 기관 재전송은 없다.
- FEP 접수가 없다는 확정 404라면 **같은 고정 보고의 등록**을 복구할 수 있다. FEP 접수가 있으면 중복 POST로 기관을 다시 호출하지 않는다. Mock 응답 유실은 조회로 회복한다.
- 기본 `pg.aml.enabled`, events-enabled, dispatch-enabled, automatic-query-enabled, retransmit-enabled는 모두 false다. `aml-test` + `skeleton`의 명시적 개발 조립만 허용하고 aws/prod/production 조합은 거부한다. `pg.aml.policy=development-v1`로 사용자 확정 개발 룰을 선택한다. 실제 기관 목적지는 허용하지 않는다.
- 실제 DB: AML V3, FEP V5 migration. 기존 Account/Ledger MariaDB 스키마와 Transfer/Payment PostgreSQL 스키마는 변경하지 않는다.
- AML/Mock KoFIU 로그의 Loki stream만 48h로 설정했다. 중앙 수집기/백업/아카이브의 실제 삭제 시한·프로비저닝은 배포 전 검증 대상이며 기존 서비스의 168h 설정은 유지한다.

## 검증 체크리스트

- [x] 실제 PostgreSQL의 경계값·다중 토픽 중복·동시성·역순·지연·규칙 버전·재평가·Outbox 실패 롤백.
- [x] 관리자/다른 호출자 차단, 잘못된 페이지/기간 차단.
- [x] FEP 동일 키/다른 내용 충돌, 접수·결과 분리, 응답 유실·조회 우선·fresh NOT_RECEIVED·간격/횟수·lease fencing·재시작.
- [x] Mock 중복 접수·프로세스 재시작·최종 결과 고정.
- [x] 수정 보고 멱등성·원보고 고정, 만료 후 원문 제거·해시 tombstone·재전달 차단.
- [x] 최종 소스 전체 Java/실제 DB/Kafka 재검증 및 분리 Docker MVP1~MVP6 결과 기록 — `infra/docker/evidence/mvp6-progress-2026-10-21.json` 참고.

검증 runner는 새 프로젝트·새 DB·새 볼륨만 사용한다. 48시간 시험은 합성 레코드의 시각을 앞당긴 검증이며 48시간 실시간 대기를 주장하지 않는다. 실제 기관·AWS·NAT·유료 리소스·LLM 호출, 커밋/push/PR/병합/배포는 수행하지 않는다.

## 남은 운영 의존성

실제 KoFIU 신고 포맷/기관 인증·규제 검증, 실제 관리자 발급/회수·감사 열람 UI, Kafka poison-event 격리/DLQ·재처리 승인, 대규모 규칙 재평가의 분할·성능/SLO, 백업/중앙 로그의 물리 삭제 SLA와 hash-only tombstone 정리 시점은 별도 운영 설계다. 현재 기능을 운영 준비 완료나 실제 AML 규제 준수로 표현하지 않는다.

최종 검증: Java 458개(실패/오류/skip 0), 분리 Docker 321개, 운영 API 9개 및 Loki 설정 검증 통과. 과거 인계 결과를 재사용하지 않았으며 세부 결과는 위 evidence에 기록했다.
