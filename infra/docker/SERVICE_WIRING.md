# SERVICE_WIRING — Docker 스택 서비스 간 REST 배선 계약

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
