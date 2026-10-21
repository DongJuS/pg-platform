# SERVICE_WIRING — Docker 스택 서비스 간 REST 배선 계약

현재 구현 주의(2026-10-20): 아래 조사 이력의 “client 미소비”, “JWKS 없음” 등은 MVP1 이전 상태다. 현재 Gateway JWT/JWKS·MVP1 client와 Payment→FEP→Mock PG, Payment→Account/Ledger가 실제 구현됐다. 최신 MVP2 경로·인증·완료 조건은 [Payment 체크리스트](../../main-vpc/core/payment/SKELETON.md)를 따른다. 기존 base-url 설정을 재사용했으며 AWS 리소스나 고객 스택을 변경하지 않았다.

Backend ①(api-gateway 라우팅 + JWT/JWKS via auth)과 ②(MVP1 이체 플로우:
account→ledger→transfer)를 구현할 때 Docker 배선을 다시 조사하지 않도록,
이 문서가 **조사 완료된 배선 계약**을 고정한다. 모든 내용은
`infra/docker/compose.yaml`, `infra/docker/config/application-docker.yml`,
`infra/docker/config/haproxy.cfg` 실물 기준이다 (2026-10-18 확인).

**선언: ①② 구현에 canonical compose/config 변경은 하나도 필요 없다.**
프로퍼티 맵, 네트워크, 호스트 접근 경로는 이미 모두 준비되어 있고,
남은 것은 각 caller 서비스의 client adapter에서 프로퍼티를 바인딩하는
Java 구현(Backend 작업)뿐이다.

---

## 1. 프로퍼티 체인 (서비스 간 base-url)

주입 경로:

```
compose.yaml x-app-environment
  ├─ SPRING_PROFILES_ACTIVE=docker,skeleton
  └─ SPRING_CONFIG_ADDITIONAL_LOCATION=file:/config/
        + 볼륨 마운트 ./config:/config:ro
              ↓
  file:/config/application-docker.yml  (on-profile: docker 에서만 활성)
              ↓
  pg.clients.<service>.base-url  (16개 전 서비스)
```

- `pg.clients` 맵 전체 목록 (기본값, env `${..._BASE_URL}`로 오버라이드 가능):

  | 프로퍼티 | 기본값 | env 오버라이드 |
  |---|---|---|
  | `pg.clients.auth.base-url` | `http://auth:8081` | `AUTH_BASE_URL` |
  | `pg.clients.account.base-url` | `http://account:8082` | `ACCOUNT_BASE_URL` |
  | `pg.clients.ledger.base-url` | `http://ledger:8083` | `LEDGER_BASE_URL` |
  | `pg.clients.transfer.base-url` | `http://transfer:8084` | `TRANSFER_BASE_URL` |
  | `pg.clients.payment.base-url` | `http://payment:8085` | `PAYMENT_BASE_URL` |
  | `pg.clients.settlement.base-url` | `http://settlement:8086` | `SETTLEMENT_BASE_URL` |
  | `pg.clients.swift.base-url` | `http://swift:8087` | `SWIFT_BASE_URL` |
  | `pg.clients.wealth-report.base-url` | `http://wealth-report:8088` | `WEALTH_REPORT_BASE_URL` |
  | `pg.clients.aml.base-url` | `http://aml:8089` | `AML_BASE_URL` |
  | `pg.clients.ai-gateway.base-url` | `http://ai-gateway:8090` | `AI_GATEWAY_BASE_URL` |
  | `pg.clients.fep-adapter.base-url` | `http://fep-adapter:8091` | `FEP_BASE_URL` |
  | `pg.clients.mock-pg.base-url` | `http://mock-pg:8092` | `MOCK_PG_BASE_URL` |
  | `pg.clients.mock-swift.base-url` | `http://mock-swift:8093` | `MOCK_SWIFT_BASE_URL` |
  | `pg.clients.mock-kofiu.base-url` | `http://mock-kofiu:8094` | `MOCK_KOFIU_BASE_URL` |
  | `pg.clients.mock-clearing.base-url` | `http://mock-clearing:8095` | `MOCK_CLEARING_BASE_URL` |

- **현재 미소비 상태 (명시)**: `application-docker.yml`의 TODO[CLIENT] 주석대로
  기존 REST skeleton 어댑터는 이 프로퍼티를 아직 바인딩하지 않는다.
  Backend가 **각 caller 자신의 adapter/out client**에서
  `@ConfigurationProperties("pg.clients")` 또는 `@Value("${pg.clients.<svc>.base-url}")`로
  바인딩해야 한다. 공용 HTTP client 추가나 FEP/AI Gateway 우회는 canonical 주석상 금지.
- migration 문서의 `pg.docker.endpoints` 표기는 느슨한 표기 — 실제 계약은 위
  `pg.clients.*` 맵이 유일하다.

## 2. 네트워크 행렬 (①② 관련)

모든 앱 서비스는 `main` 네트워크(internal)에 속하며, 컨테이너 간에는
**Compose DNS 서비스명으로 직결**한다. 포트 발행(publish)은 필요 없다.

| 호출 경로 | 대상 base-url | 네트워크 | 비고 |
|---|---|---|---|
| api-gateway → auth | `http://auth:8081` | main | JWKS 조회 포함(§4) |
| api-gateway → account | `http://account:8082` | main | 라우팅 |
| api-gateway → ledger | `http://ledger:8083` | main | 라우팅 |
| api-gateway → transfer | `http://transfer:8084` | main | 라우팅 |
| transfer → account | `http://account:8082` | main | 계좌 검증/차감 |
| transfer → ledger | `http://ledger:8083` | main | 원장 기입 |

각 호출자는 §1의 `pg.clients.<svc>.base-url`을 통해 위 URL을 얻는다.
하드코딩 금지.

## 3. 호스트 접근 (dev-access)

호스트(개발자 머신)에서의 접근은 `dev-access`(HAProxy) TCP forward만을 사용한다.
전부 `127.0.0.1` 바인딩 — 외부 노출 없음.

| 호스트 포트 | 서비스 (컨테이너 포트) |
|---|---|
| 38080 | api-gateway (8080) |
| 38081 | auth (8081) |
| 38082 | account (8082) |
| 38083 | ledger (8083) |
| 38084 | transfer (8084) |
| 38085 | payment (8085) |
| 38086 | settlement (8086) |
| 38087 | swift (8087) |
| 38088 | wealth-report (8088) |
| 38089 | aml (8089) |
| 38090 | ai-gateway (8090) |
| 38091 | fep-adapter (8091) |
| 38092 | mock-pg (8092) |
| 38093 | mock-swift (8093) |
| 38094 | mock-kofiu (8094) |
| 38095 | mock-clearing (8095) |

## 4. gateway → auth JWKS

- **현재 JWKS 엔드포인트는 존재하지 않는다.** 저장소 전체에 jwks 구현 0건 —
  auth는 JDK 기반 자체 RS256 발급(`Rs256JwtSupport`)만 가진다. JWKS 노출은
  Backend ①이 만드는 산출물이다.
- 경로가 확정되면 gateway는 **`http://auth:8081<JWKS 경로>`**로 접근한다.
  두 서비스 모두 `main` 네트워크 소속이므로 **네트워크/compose 변경 불필요**.
- JWKS 캐시 정책(TTL, kid 롤오버 처리)은 Backend 구현 사항.
- 참고: auth는 `pg.auth.jwt.private-key-pem` / `pg.auth.jwt.public-key-pem`
  미주입 시 재시작마다 임시 키를 생성해 기존 토큰이 전량 무효화된다
  (`AuthTokenProperties` dev 기본값). E2E 반복 실행성을 위한 dev 고정 키 주입은
  별도 opt-in 키트(compose.devkeys.yaml — 채택 여부는 auth 소유자 판단)로만
  제공하며, base 스택에는 포함하지 않는다. AWS는 Secrets Manager 경로를 사용한다.
- gateway 보안 현황: `/actuator/health/**` 외 deny-all
  (`GatewaySecurityConfiguration`), `RouteLocator` 미등록
  (`GatewayRoutesConfiguration` TODO) — 모두 Backend ① 작업 범위.

## 5. 변경 불필요 선언 (조사 완료)

다음은 ①② 구현에 **변경이 필요 없음을 확인 완료**한 항목이다. 변경 요구가
생기면 canonical 파일 소유자 경유로만 진행한다.

- `infra/docker/compose.yaml` — 네트워크/의존성/env 체인 이미 완비.
- `infra/docker/config/application-docker.yml` — `pg.clients` 맵 완비(16개).
- `infra/docker/config/haproxy.cfg` — dev-access 포워드 완비(38080~38095).
- `infra/docker/init/kafka-topics.sh` — MVP1 필요 토픽
  (`ledger.posted`, `transfer.completed`) 이미 생성됨.

## MVP3 전송 준비 배선 (2026-10-20)

AI Gateway `POST /internal/v1/ai/preparations`는 기존 ai/data 네트워크, ai_gateway_db, Collector를 사용한다. AI에만 `AI_PREPARATION_WEALTH_TOKEN`을 주입하며 호출자별 인증은 기존 생성 필터의 `pg.internal.require-caller-token=true` opt-in이다. 기본 호출자는 wealth-report이고 금융 공통 토큰은 AI 호출자의 신원을 증명하지 못한다. 보고서 생성/외부 추론 client는 계속 비활성이다. 분리 MVP3 시험은 AI의 ai-egress 연결까지 제거한다. 원문 마스킹·완료 의미·오류·탐지 한계는 [AI 구현 체크리스트](../../ai-vpc/ai-gateway/SKELETON.md)를 따른다.

## MVP4 개발 보고서 경로 (2026-10-20)

Gateway의 `/api/v1/wealth/reports`와 하위 PDF 경로는 기존 JWT/신원 헤더 제거 뒤 wealth-report로 전달한다. 동기 완료의 Gateway 대기는 100s, 서비스 전체 예산은 최대 90s다. Wealth → Ledger의 `POST /internal/v1/ledger/report-snapshots`는 기존 금융 내부 토큰과 wealth-report 호출자 제한을 적용하고, Ledger → Account로 소유권을 확인한 후 MariaDB의 단일 일관된 읽기를 수행한다. Wealth는 자신의 PostgreSQL에만 snapshot/report/PDF를 쓴다.

기존 compose 고객 스택은 활성화하지 않았다. 기본 후보 산식 플래그는 false이며 실제 inference port는 미설정 실패다. `run-mvp1-e2e.py --suite MVP4`만 새 프로젝트에서 `report-test` 합성 설명·후보 정책·Kafka 소비를 명시적으로 활성화한다. 실제 LLM 호출이나 S3/AWS 검증으로 표현하지 않는다. 준비 client는 별도 구현되어 있으나 생성 조립에는 등록하지 않는다. 상세 체크리스트는 `main-vpc/info/wealth-report/SKELETON.md`를 따른다.

## MVP5 개발 SWIFT 배선

기존 `pg.clients.transfer/swift/ledger/fep-adapter/mock-swift.base-url`을 재사용한다. 신규 SWIFT 내부 경로는 `pg.swift.caller-tokens.<caller>`로 호출자를 인증하고 기존 PG 경로의 금융 공통 토큰 계약은 보존한다. Swift의 외부 client는 FEP만 호출하고 FEP의 새 기관 adapter는 localhost/Mock SWIFT만 허용한다. `ledger.overseas-applied` 토픽을 추가했으며 토픽 생성과 실제 소비 계약을 구분한다.

`run-mvp1-e2e.py --suite MVP5`만 새 격리 프로젝트에서 `swift-test`·USD 계좌·SWIFT Outbox 및 2초 간격 재전송 시험 옵션을 활성화한다. 운영 재전송 기본 false이며 기본 compose 고객 스택은 활성화하지 않았다. 원문 로그는 console/중앙 로그와 분리한 private sink, 관리자 조회·일반 본인 마스킹, 48시간 보존을 적용한다. [MVP5 계약·체크리스트](../../main-vpc/external/swift/SKELETON.md)를 따른다.

## MVP6 AML 개발 배선

`run-mvp1-e2e.py --suite MVP6`는 새 격리 프로젝트에서 기존 MVP1~MVP5 회귀와 AML/FEP/Mock KoFIU를 함께 검증한다. 기존 스택·볼륨·기존 PG/SWIFT 활성화 설정은 변경하지 않는다. Ledger/Transfer/Payment의 새 AML 조회만 전용 `aml` 토큰으로 보호한다. 관리자 API는 별도 `aml-operations` 자격증명만 허용한다.

AML은 Kafka의 기존 V1을 읽고 원본 서비스 API를 조회한다. 다른 DB를 직접 읽지 않는다. 기관 경로는 AML → FEP 내부 REST → Mock KoFIU다. `aml.report-ready` 토픽은 실제 Outbox 통지용이며 사건을 발행했다고 보고 완료가 되는 것은 아니다. Mock의 새 test-owned 상태 디렉터리는 재시작 후 유지하고 시험 종료 시 해당 자원만 정리한다.

분리 검증은 사용자 확정 `development-v1` 룰을 명시 활성화하고 새 모의 계좌·개발 잔액과 실제 인증/OTP 이체 API를 사용한다. 기본 compose와 운영 profile은 자동 활성화하지 않는다. [AML 계약·정책·체크리스트](../../main-vpc/info/aml/SKELETON.md)와 `evidence/mvp6-progress-2026-10-21.json`을 참조한다.
