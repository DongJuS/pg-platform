# FEP 구현 상태

PG 경로는 MVP2에서 이미 구현됐다. `PgGatewayApplicationService`, PG REST client·receipt·서명 callback 전달·결과 조회 및 자동 금융 변경 재전송 금지 정책을 보존한다. [Payment 체크리스트](../../main-vpc/core/payment/SKELETON.md)를 따른다.

MVP5 SWIFT 경로는 `FepAdapterApplicationService.sendSwiftMessage` → `SwiftFepService` → `SwiftReceiptStore` / `SwiftGatewayPort`로 구현했다. PostgreSQL V4 스키마가 접수·시도·결과 보고서를 저장한다. 검증된 전문을 저장한 뒤 DB 트랜잭션 밖에서 Mock SWIFT에 전송하며, 중복 접수는 재전송하지 않는다. 실제 재전송은 별도 API와 기본 false 옵션이다. 결과 조회와 authenticated callback은 `FepResultController`가 처리한다.

공식 ISO pacs.008.001.08·pacs.002.001.10 XSD 사본 및 제한 부분집합으로 검증하며 전체 은행/SWIFT 호환성을 주장하지 않는다. 신규 호출자는 전용 토큰, 개인정보는 AES-GCM·48시간 보존을 적용한다. `swift-test` 전용이며 실제 기관 client는 없다. [상세 계약·검증 체크리스트](../../main-vpc/external/swift/SKELETON.md)를 참조한다.

정산 및 generic Dispatch/QueryInstitutionResult의 옛 포트는 여전히 스켈레톤이다. KoFIU의 새 실제 개발 경로는 아래 MVP6 절에 기록한다. PG/SWIFT 구현과 혼동하지 않는다.

## MVP6 KoFIU 개발 구현 (2026-10-21)

KoFIU 경로는 이제 `FepKofiuController` → `KofiuFepService` → `KofiuReceiptPort` / `KofiuGatewayPort`로 구현했다. 기존 generic 스켈레톤·정산과 구분한다. PostgreSQL V5가 보고 ID/키·고정 내용 해시·접수·시도·lease·기관 결과를 저장한다. 접수 트랜잭션 commit 후 Mock 전송하며 중복 등록은 재전송하지 않는다. POST 202는 FEP 접수이고 최종 ACCEPTED/REJECTED와 다르다.

`GET /internal/v1/fep/kofiu/reports/{key}`는 기관 결과 조회, `POST .../{key}/retransmissions`는 현재 조회에서 NOT_RECEIVED가 확인된 경우의 명시 재전송이다. 전용 `pg.aml.caller-tokens.aml`, `aml-test` 개발 profile, `pg.aml.enabled=true`가 필요하다. 실제 외부 주소·redirect를 허용하지 않는다. PG/SWIFT의 토큰·엔드포인트·재전송 정책은 유지한다. 보고 내용·시도는 AML 자체 정책으로 48시간 뒤 제거하고 hash-only tombstone으로 재접수를 차단한다. [AML 체크리스트](../../main-vpc/info/aml/SKELETON.md)를 따른다.
