# 구현·검증 보고 — 2026-10-18

작업 위치: `/Users/ddj/Documents/00_code/05_Project/sinhan_fintech/pg-platform`. 기존 빈 구조에 서비스 16개·공통 라이브러리 5개를 채웠다. MariaDB + PostgreSQL 유지 결정은 사용자에게 확인했다. 업무 로직·엔티티·업무 테이블·업무 API·Kafka 토픽은 만들지 않았다.

## 세 영역

| 영역 | 구현 |
| --- | --- |
| 스캐폴딩 | Gradle Kotlin DSL 21모듈, projectDir, Wrapper, Java21, convention3개, main·설정·빈 Flyway baseline·ArchitectureTest, CI |
| 보일러플레이트 | 순수 ErrorCode/ErrorType, 표준 오류 응답, final Money, 이벤트 계약 규칙, Outbox 인터페이스·ack 후 상태반영/retry polling, Servlet/WebFlux traceId·MDC, JSON 로그, health·Prometheus·OTel, OpenAPI, JDBC/Flyway·Kafka·Redis 설정, 공통 Testcontainers |
| 인프라 | 독립 Compose, MariaDB·PostgreSQL·Redis·Kafka·Prometheus·Grafana·Collector, Terraform6모듈·4VPC·3peering·FEP NAT·isolated data·RDS2종·Redis·MSK·비공개 암호화 PDF S3·Fargate 코드 |

## 기본값과 이유

- Java21, Boot3.5.16(요청한3.x), Gradle8.14.4(Boot지원계열), JUnit5, Testcontainers1.21.4, ArchUnit1.5.1, Cloud2025.0.3, springdoc2.8.17. 라이브러리는 TOML 한 곳에서 관리한다.
- Terraform1.16.6, AWS provider6.69.1(lock), 서울/dev. state는 S3 native lockfile. 실제 버킷·자격 증명은 미준비다.
- MariaDB11.8·PostgreSQL17. dev RDS 각각 db.t4g.micro·20GB·Single-AZ, Redis micro1개, MSK t3.small2개, Fargate ARM 0.25vCPU/512MiB·서비스당1개. 유료 토글은 기본 false다.
- 공통 웹 코드는 convention template와 shared resources에서 생성한다. HTTP 상태 매핑은 서비스 inbound web 경계에 둔다.
- Money는 정확한 통화 scale을 요구한다. 환율·수수료 반올림을 임의 확정하지 않았다.
- Outbox는 at-least-once relay다. 업무 저장 어댑터·테이블은 미포함. Gateway JWT·rate limit은 뼈대이며 health 외 요청을 기본 차단한다.

### R7 판별과 한계

main-vpc의 Java/HTTP transport API 사용 위치를 검사한다. 알려진 내부 서비스명의 outbound adapter만 허용하며 외부 PG adapter의 HttpClient 사용 음성 테스트는 실패한다. 문자열 URL·리플렉션·모든 간접 통신을 증명하지 못하므로 main↔mock 비연결·SG·FEP 출구가 함께 제한한다. 금융 대외 Kafka topic/handler/클라이언트는 만들지 않았다.

## 검증

| 실행 | 결과 |
| --- | --- |
| `./gradlew build` | 최초 전체 빌드 성공 |
| `./gradlew build -Pintegration=true --no-daemon` | 최종 성공, 테스트 27개, 실패0, skip0, 서비스 ArchitectureTest16개 |
| Testcontainers | transfer db·redis·kafka 구성 요소 각각 UP, root UP |
| `python3 load-test/verify-architecture-mutations.py` | 실제 production 임시 소스로 R1/R5 ArchUnit 실패, R6 final 상속 컴파일 실패. 제거 후 전체 build 재통과 |
| Compose | 저장소·관측성 실행, transfer·account health UP, 업무 테이블 없이 Flyway metadata만 존재 |
| HTTP·관측성 | traceId/MDC·HTTP매핑/405 테스트, 실제404 COMMON-HTTP-404, JVM metrics, Prometheus transfer/otel UP, Grafana database ok, Collector trace 수신 |
| `terraform fmt -check -recursive infra/terraform` | 통과 |
| `terraform -chdir=infra/terraform/envs/dev validate` | 통과 |
| `terraform plan` | AWS 자격 증명 없어 생략. STS 읽기 확인에서 credentials unavailable |
| `terraform apply` | 실행하지 않음 |
| 폴더 대조 | 누락0, 추가 서비스·라이브러리 모듈0. convention/template·운영 파일만 추가 |

R6는 final 때문에 실제 소스에서 ArchUnit 이전에 컴파일러가 차단한다. 별도로 격리된 비-final Money 대역 fixture로 R6 ArchUnit subclass 탐지도 확인했다. 운영 Money를 비-final로 바꾸지 않았다.

증거: verification-r1/r5/r6-expected-failure.log, verification-architecture-mutations.json, verification-gradle-build.log, verification-terraform-validate.log. 구조화 요약은 verification-summary.json에 있다.

## 비용·질문

AWS 리소스 생성은 없다. 전부 켠 유휴 dev는 월 약 **$650~750**, 기본 유료 토글 off 유휴 구성은 월 약 $0~1이다. 730시간·세금/대규모 트래픽 제외 기준이다. endpoint32개 AZ 단위 약 $303.68가 가장 큰 고정비다. [AWS_COST.md](AWS_COST.md)에 단가·계산이 있다.

외부 LLM 경로, Redis IAM token provider, 실제 secret/image/backend/ACM, AWS 관측성 주소는 [OPEN_QUESTIONS.md](OPEN_QUESTIONS.md)에 남겼다. NAT 추가·망 합치기·Kubernetes·비밀값 저장·AWS apply로 임의 해결하지 않았다.

## 폴더 트리 — 디렉터리 3단계, build/cache 제외

```text
pg-platform/
├─ .github/
│  └─ workflows/
├─ ai-vpc/
│  └─ ai-gateway/
│     └─ src/
├─ build-logic/
│  ├─ .kotlin/
│  │  └─ sessions/
│  └─ src/
│     └─ main/
├─ docs/
│  ├─ adr/
│  ├─ api/
│  └─ prd/
├─ fep-vpc/
│  └─ fep-adapter/
│     └─ src/
├─ gradle/
│  └─ wrapper/
├─ hooks/
├─ infra/
│  ├─ docker-compose/
│  ├─ observability/
│  └─ terraform/
│     ├─ envs/
│     └─ modules/
├─ libs/
│  ├─ arch-rules/
│  │  └─ src/
│  ├─ common-error/
│  │  └─ src/
│  ├─ common-money/
│  │  └─ src/
│  ├─ event-contracts/
│  │  └─ src/
│  └─ outbox/
│     └─ src/
├─ load-test/
├─ main-vpc/
│  ├─ channel/
│  │  └─ api-gateway/
│  ├─ core/
│  │  ├─ account/
│  │  ├─ auth/
│  │  ├─ ledger/
│  │  ├─ payment/
│  │  ├─ settlement/
│  │  └─ transfer/
│  ├─ external/
│  │  └─ swift/
│  └─ info/
│     ├─ aml/
│     └─ wealth-report/
├─ mock-vpc/
│  ├─ mock-clearing/
│  │  └─ src/
│  ├─ mock-kofiu/
│  │  └─ src/
│  ├─ mock-pg/
│  │  └─ src/
│  └─ mock-swift/
│     └─ src/
└─ skills/
```
