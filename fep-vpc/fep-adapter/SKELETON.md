# FEP 구현 상태

PG 경로는 MVP2에서 이미 구현됐다. `PgGatewayApplicationService`, PG REST client·receipt·서명 callback 전달·결과 조회 및 자동 금융 변경 재전송 금지 정책을 보존한다. [Payment 체크리스트](../../main-vpc/core/payment/SKELETON.md)를 따른다.

MVP5 SWIFT 경로는 `FepAdapterApplicationService.sendSwiftMessage` → `SwiftFepService` → `SwiftReceiptStore` / `SwiftGatewayPort`로 구현했다. PostgreSQL V4 스키마가 접수·시도·결과 보고서를 저장한다. 검증된 전문을 저장한 뒤 DB 트랜잭션 밖에서 Mock SWIFT에 전송하며, 중복 접수는 재전송하지 않는다. 실제 재전송은 별도 API와 기본 false 옵션이다. 결과 조회와 authenticated callback은 `FepResultController`가 처리한다.

공식 ISO pacs.008.001.08·pacs.002.001.10 XSD 사본 및 제한 부분집합으로 검증하며 전체 은행/SWIFT 호환성을 주장하지 않는다. 신규 호출자는 전용 토큰, 개인정보는 AES-GCM·48시간 보존을 적용한다. `swift-test` 전용이며 실제 기관 client는 없다. [상세 계약·검증 체크리스트](../../main-vpc/external/swift/SKELETON.md)를 참조한다.

KoFIU·정산 및 generic Dispatch/QueryInstitutionResult의 옛 포트는 여전히 스켈레톤이다. PG/SWIFT 구현과 혼동하지 않는다.
