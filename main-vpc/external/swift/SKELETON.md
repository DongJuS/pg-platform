# MVP5 SWIFT 구현·검증 체크리스트

인계 기준 `main@d4598f760bd24583903ccd122f858bff1bf45f06`. 구현은 개발용 가상 송금이며 실제 은행·SWIFT망·AWS·NAT·유료 리소스를 연결하지 않는다. 커밋·PR·병합은 수행하지 않는다. 최신 검증 수치와 한계는 [검증 기록](../../../infra/docker/evidence/mvp5-progress-2026-10-20.json)을 따른다.

## 확정 범위와 설계 대조

- 사용자 확정: USD 소수 2자리만, 환전 없음, 수수료 0. 접수 시 고객 → 송금별 대기 계정, ACSC 시 대기 → 개발 정산 계정, RJCT 시 대기 → 고객 전액 반환. 운영 활성화는 보류.
- 사용자 확정: ACSC는 외부 완료, RJCT는 외부 거절. ACTC/ACCP/ACSP/PDNG/RCVD는 진행. 알 수 없는 코드·상충 결과는 REVIEW. 원장 반영 전 고객 완료를 표시하지 않는다. 상충 결과는 새 분개·자동 보상을 만들지 않는다.
- 최신 사용자 지시로 수동 결과 조회·실제 재전송 API와 관리자 원문 조회를 구현했다. 최초 요청의 F07 보류보다 이 지시를 우선하며, 옛 `SwiftOperationsController` 자리 대신 `SwiftRecoveryController`와 Transfer의 `OverseasOperationsController`를 사용한다.
- 원문·수취인 정보는 암호화 저장·관리자 감사 조회·분리된 관리자 로그에 포함한다. 일반 사용자는 본인 송금의 마스킹 기록만 조회한다. 민감 데이터·전문·감사 보존은 48시간. 금융 식별자·분개·최종 결과·멱등 해시는 복구용으로 별도 유지한다.
- Figma MCP는 Starter 한도로 실패했지만 연결된 Chrome에서 `32:14` 및 주변 ‘SWIFT 전문 파서 / 해외송금 요청 수신 / MT103 파서·매핑 / FEP 전달 요청 / pacs.002 응답 처리 / fep.ack 발행’을 읽었다. `19:2` 시스템 구성도, `2:2` 전체 구성도와 `24:42`도 확인했다.
- `32:14`는 FEP REST와 무응답 시 같은 UETR 재전송을 표시한다. 횟수·간격·재조회 정책은 도식에 없어 개발값(아래)과 구분한다. 이전 코드는 실제 소비·저장·송신이 없는 스켈레톤이었다.
- `24:42`의 Ledger → transfer.completed, Redis 멱등 잠금 및 PostgreSQL 표기는 현재 확정 ADR/구현과 다르다. Ledger → ledger.posted, Transfer → transfer.completed, DB unique 및 MariaDB(Account/Ledger)를 유지한다. Figma는 수정하지 않았다.
- Notion PRD `3faad9292a49813bacbef22a4476700a`와 기능명세 `3fbad9292a498092bd8af323af928ca0`는 보조 대조했다. 과거 7서비스/단일 PostgreSQL/미정 잠금은 최신 사용자·ADR보다 우선하지 않는다. PDF/unknown 블록 및 외부 AI 리뷰는 확인 필요.

## 구현 체크리스트

- [x] F01/F02 JWT Gateway 신원, Account 원본 소유권·활성·통화 검사, 실제 Auth OTP 소비, 정확 금액 검증, 요청별 DB unique·내용 충돌 검사.
- [x] `overseas_transfers`는 국내 `transfers`와 분리한다. 동일 요청은 동일 transferId/UETR. OTP는 수취인·BIC·금액 전체의 결정적 식별자에 결합한다.
- [x] Ledger에 없던 가처리를 실제 추가했다. MariaDB의 계좌 순서 잠금·잔액 검사·분개·송금 예약·ledger.posted는 같은 로컬 트랜잭션. 기존 예약 확인은 현재 계좌의 신규 출금 허용 여부와 구분한다.
- [x] Transfer는 원장 예약 확인 뒤 상태와 transfer.outbound Outbox를 같은 PostgreSQL 트랜잭션에 저장한다. 응답 유실은 UNKNOWN, 예약 확인 재시도는 같은 업무 ID로만 수행한다.
- [x] 새 SWIFT 소비자 그룹은 보관된 사건부터 읽으며 기존 커밋 위치는 유지한다. ReportConsumerReplayIT는 MVP4의 늦은 새 그룹 사건 누락을 실제 Kafka에서 재현·수정 검증한다.
- [x] F03 Swift가 transfer.outbound를 실제 소비하며 전송 전에 고정 전문을 저장한다. UETR은 Transfer에서 한 번 생성한다. 재전달은 새 UETR·새 송신을 만들지 않는다.
- [x] F04 Swift → FEP REST → Mock SWIFT만 허용한다. FEP의 PG 구현과 기존 결제 재전송 금지는 보존했다. 신규 SWIFT 내부 API는 호출자별 전용 토큰을 요구하며 금융 공통 토큰으로 관리자 신원을 주장할 수 없다.
- [x] F05 결과 재조회와 실제 재전송은 별도 API다. 기본 실제 재전송 false. 운영자 명시 호출 + true 옵션 + UNKNOWN + 결과 조회에 알려진 응답 없음 + 같은 저장 전문 + 간격/횟수 제한일 때만 전송한다. 자동 금융 재전송은 없다.
- [x] F06 pacs.002 XSD·원본 message reference / EndToEndId / TxId / UETR를 검사한다. 보고서 ID와 결과 해시 inbox, 최종 결과 및 fep.ack Outbox는 같은 Swift DB 트랜잭션이다.
- [x] F08 Ledger가 fep.ack를 소비하고 저장된 예약으로만 확정/역분개한다. ledger.overseas-applied를 같은 트랜잭션에 기록한다. Transfer는 이 증거로 최종 상태와 transfer.completed를 기록한다.
- [x] Swift/ Ledger 이벤트 소비자의 ByteArrayDeserializer를 실제 Outbox byte wire와 일치시켰으며 SwiftConsumerWireIT가 모듈 YAML·실제 브로커·실제 소비자 메서드의 연결을 검증한다.
- [x] Gateway의 검증된 X-User-Id 헤더를 국내 이체와 같은 방식으로 수용한다.
- [x] FEP의 미완료 송신 lease도 결과 조회의 transmissionStatus=UNKNOWN으로 구분한다. 전송 직후 중단된 SENDING은 lease 만료 후 UNKNOWN으로 바뀌며 자동 재송신하지 않는다. 새 인스턴스는 저장된 전문·식별자로 결과 조회를 재개한다.
- [x] 새 금융 소비자는 일시 장애 뒤 기본 retry 소진으로 사건을 건너뛰지 않고 durable commit 전 ack하지 않는다. 유한 poison-event/DLQ 운영 정책은 별도 미정이다.
- [x] Gateway 서비스 DNS cache TTL을 기본 5초(설정 1~60초)로 제한해 동시 컨테이너 재시작의 주소 변경을 수용한다. 금융 HTTP 변경 재전송을 추가하지 않는다.
- [x] 전문 암호화(AES-256-GCM, 행 ID AAD), XML 크기 제한, DTD/외부 엔티티·외부 XSD 차단, 고정 오류 메시지, 원문 로그 분리, 48시간 조회 제한·삭제 및 Swift 미완료 전문 만료 시 REVIEW를 구현했다.

## 전문 지원 범위

`libs/swift-messages`는 순수 Java wire 검증·암호화 유틸이며 서비스 업무·DB를 공유하지 않는다. MT103 **block 4의 제한된 개발 부분집합**: 20, 고정 23B=CRED, 32A, 50K(계좌+한 줄 이름), 57A(BIC), 59(계좌+한 줄 이름), 선택 70, 71A=SHA. 중복·미지원·역순 필드·불가능한 날짜·정밀도 손실은 거절한다. FIN 헤더/트레일러 전체나 자유 형식 주소는 지원하지 않는다. 발신자는 현행 신청 API에 실명 필드가 없어 명시적으로 `SYNTHETIC SENDER`를 사용한다. 실명 송금으로 주장하지 않는다.

pacs.008.001.08 / pacs.002.001.10의 **완전한 공개 XSD 사본**으로 검사하고 추가로 단일 거래·USD·무환전·지원 필드만 허용한다. [ISO 공식 아카이브](https://www.iso20022.org/catalogue-messages/iso-20022-messages-archive?search=pacs.008)에서 두 버전을 확인했다. 공식 바이너리 다운로드는 403이어서 Moov의 공개 generated ISO 사본을 변경 없이 사용했다. URL·파일 크기·SHA-256은 [SOURCES.json](../../../libs/swift-messages/src/main/resources/xsd/SOURCES.json)에 고정한다. 간이 XSD가 아니지만 CBPR+ market practice, BAH, 실제 은행 라우팅·수수료·규제·SWIFT 인증을 검증했다는 의미는 아니다.

## API·권한

| API | 입력 / 응답 | 권한 |
| --- | --- | --- |
| POST /api/v1/transfers/overseas | Idempotency-Key, fromAccountId, recipientName, recipientAccount, bic, amount, currency, otpToken → 202 transferId/uetr/status | 본인 출금 계좌 + JWT + OTP |
| GET /api/v1/transfers/overseas/{id} | 본인 상태·UETR·고정 거절 코드 → 200 | 본인 |
| GET /api/v1/transfers/overseas/{id}/messages | 48h 내 감사·전문 이력 → 200, no-store | 일반: 본인 마스킹 / 관리자: 원문 |
| POST /api/v1/transfers/overseas/{id}/result-queries | 외부 상태 재조회 → 200 externalStatus | 관리자 |
| POST /api/v1/transfers/overseas/{id}/retransmissions | 명시 재전송 시도 → 200 externalStatus 또는 정책상 409 | 관리자 + 옵션 true |
| GET /internal/v1/transfers/overseas/{id}/instruction | 원본 고정 개발 송금 정보 | swift 전용 토큰 |
| POST /internal/v1/ledger/overseas-reservations | transferId/uetr/actor/source/amount/currency → hold journal receipt | transfer 전용 토큰 |
| POST /internal/v1/fep/swift/messages | transferId/uetr/messageKey/pacs008 → 202 messageId/acceptanceStatus | swift 전용 토큰 |
| GET /internal/v1/fep/swift/messages/{uetr}/result | 검증된 pacs.002 보고서 목록 | swift 전용 토큰 |
| POST /internal/v1/fep/swift/messages/{uetr}/retransmissions | 같은 저장 전문 재전송 | swift 전용 토큰 + 옵션 |
| POST /internal/v1/fep/swift/messages/{uetr}/results | pacs002 callback | mock-swift 전용 토큰 |
| POST /internal/v1/swift/transfers/{uetr}/result-queries 또는 /retransmissions | 조회 / 명시 재전송 | swift-operations 전용 토큰 |
| GET /internal/v1/swift/transfers/{uetr}/messages | 원문·시도·결과 이력 | swift-operations 전용 토큰 |

오류: 인증 401, 권한 403, 부재/만료 404, 내용 충돌·금지 재전송 409, 입력/양식 400, 일시 불명 503. 외부 접수나 송신 API 200/202는 금융 성공이 아니다. 송금의 원장 잔액 부족은 FAILED로 저장하며 새 금융 효과를 만들지 않는다. 내부 SQL/외부 XML 파서 오류·전문·계좌·토큰은 응답에 포함하지 않는다.

관리자는 검증된 Gateway actor UUID와 `pg.swift.admin-user-ids`의 서버 설정으로 판별한다. 요청의 X-Role/X-User-Id는 승격 수단이 아니다. 운영자 API의 토큰은 일반 금융 토큰과 분리한다. 실제 조직 관리자 프로비저닝·역할 변경 UI는 구현하지 않았다.

## 개발 옵션과 실행

- `pg.swift.enabled=false` 기본. true라도 `swift-test` 프로필 필수이며 aws/prod/production 조합은 기동 거절. Account의 USD 개설도 이 개발 게이트에서만 허용한다. 기본 MVP1 KRW 정책은 그대로다.
- `pg.swift.retransmit-enabled=false` 기본. 개발 최대 송신 시도 3회(최초 포함), 최소 재전송 30초. 값 범위는 1~10회, 1~86400초로 제한한다. 분리 시험만 2초를 사용한다.
- `pg.swift.recovery-interval-ms=10000`, 각 결과 재조회 10초. 초기 전송/복구 lease 30초. 결과 없음/404는 미처리·실패의 증거가 아니다.
- `pg.swift.payload-key`: 별도 생성한 base64 32바이트 AES 키. 기본 키 없음. `pg.swift.caller-tokens.<caller>` 및 `pg.swift.mock-token`도 기본 없음.
- `pg.swift.admin-user-ids`: 서버 관리자가 지정하는 UUID 목록. `SWIFT_PRIVATE_LOG_DIR`의 원문 로그는 console/중앙 로그와 분리한다. 시간별 회전, 최대 47개 이전 시간 파일, 1분 idle 회전 tick으로 최대 48시간 정책을 적용한다. 실행 중 DB 삭제는 최대 1분 지연될 수 있지만 만료 즉시 조회를 차단한다.

```sh
python3 infra/docker/run-mvp1-e2e.py --suite MVP5 --output /private/tmp/mvp5-result.json
```

위 runner만 새 프로젝트·임의 포트·전용 자격증명·Mock 상태/로그 디렉터리를 만든다. 고객 스택/볼륨은 삭제하지 않는다. 모든 테스트 자원 정리와 기존 컨테이너 보존을 결과 JSON에 기록한다.

## 검증 체크리스트와 남은 의존성

- [x] Java 공식 XSD 검증, MT 부분집합, XXE 차단, 암호화 identity 검증.
- [x] 실제 PostgreSQL/MariaDB에서 동시 요청, 멱등 효과, 역순·중복·상충·지연 결과, 로컬 상태/Outbox 롤백, 저장된 송신 중단/응답 유실 상태 및 인스턴스 재생성.
- [x] 최종 전체 Java/실제 DB/Kafka 410개, 분리 Docker 271개(MVP5 58개 포함), 운영 API 9개 통과. 최종 Docker 소스 hash는 현재와 일치한다.
- [ ] 실제 금융기관 호환성·운영 보안/부하/보존 인프라. 운영 활성화는 사용자 지시대로 보류.
- [ ] 실제 환율·환전·수수료·지원 통화 확대 및 실제 발신인·주소·market practice. 현재 범위 밖.
- [ ] poison event의 운영 재처리/DLQ와 장기 감사 정책. Kafka 기본 재전달과 DB 멱등성 검증을 운영 정책 완료로 표현하지 않는다.

Mock 정상·거절·지연·무응답·처리 후 응답 유실·동일 UETR 재송신 시험과 실제 은행 결과를 구분한다. Ledger 응답 유실은 격리 전용 HTTP 프록시에서 실제 MariaDB 커밋 후 Ledger HTTP 200을 받은 뒤 Transfer 응답 연결을 끊어 시험한다. 재시작·같은 식별자 복구를 확인하고, 별도로 결과 불명 checkpoint 주입 시험도 구분해 기록한다.
