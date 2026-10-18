---
status: accepted
date: 2026-10-18
---

# 금융 서비스 통신과 정합성 구현 기준

최신 Figma의 시스템 아키텍처 구성도를 검토한 뒤 사용자가 1-A, 2-B, 3-A, 4-A, 5-A, 6-A를 선택했다. 아래는 확정한 설계이며 구현 완료를 뜻하지 않는다. 기존의 FEP 요청 Kafka 일괄 적용과 NAT FEP 단독 배치 지침을 대체한다.

| 번호 | 확정 결정 | 선택에 따른 영향 |
| --- | --- | --- |
| 1-A | SWIFT·PG·AML·정산의 금융 대외 요청은 호출자 → FEP의 내부 REST로 통일한다. | 접수 응답과 외부 기관의 최종 결과를 구분한다. 완료 사건의 Kafka 전파는 유지하며 타임아웃·재조회·재전송은 동일 업무 식별자로 처리한다. |
| 2-B | Ledger는 `ledger.posted`, Transfer는 이체 상태를 확정한 후 `transfer.completed`를 발행한다. | 원장은 분개, 이체 서비스는 이체 완료를 책임진다. 각 서비스의 상태 변경과 Outbox 저장은 각각의 로컬 DB 트랜잭션으로 묶는다. |
| 3-A | AI VPC에 FEP와 독립된 NAT 출구를 구성한다. | 기존 FEP NAT는 금융 대외 통신에 유지한다. AI의 허용 목적지는 별도 통제로 제한하고 data 서브넷의 인터넷 기본 경로는 두지 않는다. NAT 추가에 따른 비용은 재산정한다. |
| 4-A | 보고서 API는 AI 초안 검증·PDF 생성·저장까지 기다린 뒤 완성 결과를 응답한다. | 성공 응답에는 생성된 보고서 접근 정보를 제공한다. API 동기 응답과 별개로 처리 단계·기준 시점·복구 상태는 저장한다. Kafka 스냅샷 갱신은 별도 기능이다. |
| 5-A | Outbox는 DB 폴링 방식으로 시작한다. | 기존 relay 방향을 유지한다. 전달 중복을 허용하는 at-least-once 전제로 소비자 중복 방지를 구현하고 배치 크기·주기·동시성을 부하 측정으로 조정한다. CDC 도입은 이번 결정에 포함하지 않는다. |
| 6-A | Ledger의 동일 계좌 동시 출금은 비관적 행 잠금으로 제어한다. | 계좌 ID의 일정한 순서로 잠그고, 잔액 검사·분개·잔액 갱신·Outbox 기록을 짧은 로컬 트랜잭션에서 처리한다. 교착·잠금 대기 시 제한된 재시도 정책을 둔다. |

MariaDB(account·ledger) + PostgreSQL(나머지)과 기존 배포 순서는 유지한다. 두 DB와 HTTP 호출 전체가 하나의 일반 `@Transactional`로 묶인다고 가정하지 않는다. 원장 반영 후 응답이 유실되면 기존 거래 ID로 결과를 확인하며, Transfer를 즉시 실패로 확정하거나 출금을 새 거래로 반복하지 않는다. REST 응답과 `ledger.posted` 소비가 같은 완료 처리를 중복 수행하지 않도록 상태 확정·이벤트 기록을 멱등하게 만든다.

보고서의 동기 응답은 HTTP 연결을 무기한 유지한다는 뜻이 아니다. LLM·PDF·저장 단계와 Gateway/서버의 대기 예산, 동시 생성 수 제한을 함께 정한다. 시간 초과를 완료로 응답하거나 자동으로 202 작업 접수 계약으로 바꾸지 않는다. HTTP 상태·필수 필드·이벤트 payload는 후속 계약 작업이다.

최종안의 유스케이스·port·adapter·profile 조립을 Java 스켈레톤에 반영했고 R7·내부 FEP SG·독립 AI NAT 인프라 코드도 반영했다. 금융 업무 본문·저장 SQL·실제 외부 client는 미구현이며 AWS 리소스를 생성하지 않았다. 구조 반영과 실제 동작 검증은 아래에서 구분한다.

## 반영 체크리스트

- [x] 여섯 선택을 accepted 결정으로 기록하고 프로젝트 진입 문서에 연결한다.
- [x] FEP Kafka 일괄 요청·AI 출구 미정이라는 현재 안내를 정정한다.
- [x] 일반 서비스 11개의 유스케이스 53개와 API/내부 파일을 HTTP·Kafka·배치 역할별로 구조화한다.
- [x] SWIFT·Payment·AML·Settlement의 호출자 쪽 adapter/out/fep와 FEP 내부 접수·결과 수신·결과 조회 위치를 둔다.
- [x] R7에 내부 FEP REST client 경계를 허용하고 외부 기관 직접 client는 거부하는 검사를 통과한다.
- [x] main → FEP 내부 REST 8091 SG와 기존 직접 피어링 경로를 대조한다.
- [x] 원장/이체의 이벤트 책임과 원자적 로컬 write port를 분리하고 추적 ID·멱등키·거래 ID·eventId를 구분한다.
- [x] AI 전용 NAT·app 전용 라우팅·목적지 CIDR 제한 코드를 추가하고 기본 비활성/기본 차단을 유지한다.
- [x] 정확한 LLM HTTPS hostname 검사 위치와 기본 빈 허용 목록을 만든다.
- [x] NAT 2개를 반영한 개발 환경 비용의 범위 추정을 갱신한다.
- [x] 보고서 동기 생성과 별도 스냅샷 갱신을 나누고 시간·동시성 설정의 값 검증을 추가한다.
- [x] Outbox 폴링의 batch/lease/ack 시간 설정을 분리한다.
- [x] API·내부 파일 확장 후 빌드 69개 테스트를 통과한다. 이전 인프라 검증(Terraform fmt/validate, mock 6개)과 CI 연결은 보존한다.
- [ ] 금융 대외 REST의 서비스 인증·HTTP/이벤트 필드·callback/결과 전파 wire 계약과 실제 client를 구현한다.
- [ ] 원장/이체의 로컬 Outbox 저장과 중복 완료 방지의 실제 DB 원자성을 구현한다.
- [ ] 원장 성공 후 응답 유실·이벤트 중복·서비스 재시작 복구를 검증한다.
- [ ] LLM client에 hostname 검사·redirect 차단·timeout을 연결하고 실제 제공사 목적지·인증을 설정한다.
- [ ] 보고서 단계별 제한과 동시 생성 수 제한을 집행하고 Gateway 시간 예산과 연결한다.
- [ ] Outbox 폴링을 목표 부하로 측정하고 미발행 누적·중복·전달 지연을 튜닝한다.
- [ ] MariaDB의 잠금 SQL·단일 거래 중복 방지·동시 출금·교착 복구를 구현·검증한다.
- [ ] Figma의 Ledger transfer.completed 표기를 이 결정의 발행 주체에 맞춰 수정하고 나머지 표기를 대조한다.

## 검토 근거

- [Figma 시스템 아키텍처 구성도](https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2)
- [Transfer → Ledger 동기 REST 및 기존 원장 이벤트 표기](https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=24-42)
- [SWIFT의 FEP REST 전송](https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=32-14)
- [AI NAT egress 표기](https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=26-41)
- [보고서 생성과 스냅샷 소비](https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=29-2)

Figma 조회 당시의 발행 주체와 위 2-B가 다르며, 동기 보고서 응답·폴링·잠금 방식의 상세는 사용자 선택으로 확정했다. 이 기록과 Figma의 차이는 미반영 작업으로 구분한다.
