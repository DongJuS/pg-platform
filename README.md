# pg-platform

Java 21 / Spring Boot 3.5 기반 금융 MSA 스캐폴딩. 서비스 16개와 공통 라이브러리 5개의 Gradle Kotlin DSL 멀티모듈이다. 업무 로직·엔티티·업무 테이블·업무 API·Kafka 토픽은 구현하지 않았다.

| 폴더 | 내용 |
| --- | --- |
| main-vpc/channel | api-gateway |
| main-vpc/core | auth, account, ledger, transfer, payment, settlement |
| main-vpc/external | swift |
| main-vpc/info | wealth-report, aml |
| ai-vpc | ai-gateway |
| fep-vpc | fep-adapter |
| mock-vpc | mock-pg, mock-swift, mock-kofiu, mock-clearing |
| libs | common-error, common-money, event-contracts, outbox, arch-rules |
| infra | terraform, docker-compose, observability |
| build-logic | convention plugin·공통 웹 템플릿·공통 설정 |
| load-test | 아키텍처 위반 주입·정리 검증. 업무 부하 시나리오는 아직 없음 |
| docs | prd, api, adr |

## 빌드

JDK 21이 필요하다. 이 macOS의 Homebrew JDK는 시스템 java에 등록되지 않아 아래 경로를 사용했다.

```sh
export JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
./gradlew build
```

라이브러리 버전은 `gradle/libs.versions.toml` 한 곳에서 관리한다. Boot는 요청한 3.x의 3.5.16, Gradle은 지원 계열인 8.14.4다. Wrapper 배포 체크섬도 고정했다.

[확정 설계와 반영 체크리스트](docs/adr/0001-financial-integration-decisions.md)를 변경 기준으로 사용한다. 설계 확정과 코드 구현 상태는 구분한다.

## 스켈레톤 코드

각 서비스에 Controller·요청/응답 DTO·ApplicationService·도메인·Policy·Repository·PersistenceAdapter·Entity·Mapper 파일과 구현 주석을 뒀다. API 41개 중 36개는 skeleton 프로필에서만 연결하고 5개는 보류 주석으로 남겼다. 53개 유스케이스의 조립 위치를 명시했다.

[구조 안내](docs/prd/SKELETON_GUIDE.md) · [API별 파일 목록](docs/prd/API_SKELETON_INDEX.md) · [기능 ID 체크리스트](docs/prd/FEATURE_FILE_CHECKLIST.md)

미구현 업무는 501로 표시하며 기본/local/aws에는 신규 Controller를 등록하지 않는다. 인증·금융 로직·ORM/SQL·실제 외부 client는 TODO다. Entity 필드와 API 정상 응답 표기는 초안이며 구현/검증 완료를 뜻하지 않는다.

## 로컬 실행

`infra/docker-compose/.env.example`에는 이름만 있다. 실제 값은 저장소 밖 env 파일이나 로컬 환경에서 제공한다. 검증에 쓴 일회성 env 파일은 `/private/tmp/pg-platform-local.env`이며 소스에 포함하지 않았다.

```sh
docker --context desktop-linux compose -f infra/docker-compose/compose.yaml --env-file /path/to/local.env up -d --wait
./gradlew :transfer:bootJar
docker --context desktop-linux compose -f infra/docker-compose/compose.yaml --env-file /path/to/local.env --profile smoke up -d transfer
curl http://localhost:18084/actuator/health
```

MariaDB 연결은 `./gradlew :account:bootJar` 후 같은 Compose에서 `--profile db-smoke up -d account`로 확인한다. account health는 `http://localhost:18082/actuator/health`다. 두 서비스 모두 빈 Flyway 기준선만 적용한다.

| 항목 | 로컬 주소 |
| --- | --- |
| PostgreSQL: 계좌·원장 제외 9개 서비스 DB | localhost:25432 |
| MariaDB: account_db, ledger_db | localhost:23306 |
| Redis | localhost:26379 |
| Kafka | localhost:29092 |
| Prometheus | http://localhost:29090 |
| Grafana | http://localhost:23000 |
| OpenTelemetry | gRPC localhost:24317, HTTP localhost:24318 |
| transfer | http://localhost:18084 |

Compose 프로젝트는 `pg-platform-local`이다. 이전 `sinhan_project`와 포트·볼륨이 분리돼 있다. Kafka 초기화 helper는 새 전용 볼륨 소유자만 uid 1000으로 맞추고 종료한다. Kafka는 비특권 사용자로 실행한다.

Testcontainers 검증:

```sh
export DOCKER_HOST="unix://$HOME/.docker/run/docker.sock"
./gradlew build -Pintegration=true
```

기본 build는 단위·ArchUnit 검사다. `-Pintegration=true`일 때 PostgreSQL·Kafka·Redis 테스트도 실행하며 CI는 이 옵션을 켠다.

## 계층·공통 코드

- 일반 서비스는 domain / application(port in·out) / adapter(in·out) / config. domain은 순수 Java다.
- gateway와 mock은 R1~R4 예외다. R5~R7은 이들에도 검사하며 R7 외부 통신 제한은 main-vpc에서 활성화한다.
- 내부 REST 클라이언트는 호출자 `adapter/out/<대상서비스>`에 둔다. 공용 client 모듈은 없다. 금융 대외 요청은 내부 REST → FEP로 통일했다. 호출자 `adapter/out/fep`의 client 스켈레톤과 R7 허용 경계를 추가했다. 실제 HTTP 호출·인증·wire 계약은 후속 구현이다.
- 업무 규칙은 libs에 두지 않는다. common-error는 Spring·HTTP와 분리했다. HTTP 매핑은 웹 경계에 둔다.
- 웹 공통 코드는 `build-logic/src/main/templates`에서 관리하고 각 서비스 `build/generated/platform`으로 생성한다. 생성물을 직접 수정하지 않는다. 공통 설정은 shared-resources에 있다.
- Money는 final·불변이다. 통화 scale에 정확히 맞는 값만 허용하는 UNNECESSARY 규칙을 한 곳에 둔다. 환율·수수료 계산을 정한 것은 아니다.
- Outbox는 인터페이스·폴링 relay와 batch/lease/ack 시간 설정이 있다. 업무 변경과 같은 트랜잭션에서 기록할 write port를 구조화했으며 실제 저장 어댑터·트랜잭션은 후속이다. `outbox.enabled`는 기본 꺼짐이다. at-least-once이므로 소비자 DB unique가 필요하다.
- JWT는 자체 Auth의 RS256 전제다. 발급·검증·철회는 TODO이며 Gateway는 health 외 요청을 기본 차단한다. Keycloak은 없다.

R7은 main-vpc에서 Java/HTTP transport API 사용 위치를 검사한다. 알려진 내부 서비스 이름의 outbound adapter만 허용한다. 문자열 URL·리플렉션·모든 간접 통신을 증명하는 검사는 아니다. 금융 대외 경로는 main↔mock 비연결, 내부 FEP 8091 SG, FEP 출구가 함께 제한한다. AI의 외부 LLM 출구는 별도 AI NAT·목적지 제한으로 구분한다.

`python3 load-test/verify-architecture-mutations.py`는 실제 소스에 R1/R5 위반과 final Money 상속을 넣어 실패를 확인하고 finally에서 삭제한다. 실행 후 build로 정상 상태를 다시 확인한다.

## Terraform

ap-northeast-2, dev 하나. Terraform 1.16.6, AWS provider 6.69.1(lock). network·peering·data·compute·security·storage 모듈이다.

```sh
terraform -chdir=infra/terraform/envs/dev init -backend=false
terraform fmt -check -recursive infra/terraform
terraform -chdir=infra/terraform/envs/dev validate
```

실제 state는 기존 암호화 S3 버킷과 backend.hcl.example의 native lockfile을 사용한다. 실제 AWS 자격 증명이 없어 실제 리소스의 plan은 생략했고 apply는 실행하지 않았다. 모듈 검증의 mock plan은 실제 AWS를 호출하지 않는다.

FEP NAT(`enable_nat`)·AI NAT(`enable_ai_nat`)·RDS·Redis·MSK·Fargate·interface endpoint 토글은 기본 꺼짐이다. main↔ai, main↔fep, fep↔mock만 피어링한다. NAT는 각 VPC의 app에 연결하고 data 서브넷에는 인터넷 기본 경로를 두지 않는다. AI 외부 목적지는 `ai_llm_egress_cidrs`가 비어 있으면 차단한다. EKS·Argo CD는 없다.

RDS는 MariaDB 11.8(account/ledger), PostgreSQL 17(나머지), 각각 db.t4g.micro·20GB·Single-AZ다. patch는 리전 지원 버전을 따른다. Redis cache.t4g.micro 1개, MSK 3.9.x kafka.t3.small 2개, Fargate ARM 0.25vCPU/512MiB·서비스당 1개다. 대규모 트래픽 성능 합격을 의미하지 않는다.

Secrets Manager 이름/ARN metadata만 참조하며 값은 Terraform으로 읽지 않는다. 배포 시 ECS에서 주입한다. Redis IAM client와 이미지·secret 이름·인증서가 미준비면 precondition으로 compute 활성화를 막는다.

[API·내부 파일 스켈레톤 검증](docs/prd/verification-api-skeleton.json) · [최초 보일러플레이트 기록](docs/prd/SCAFFOLDING_REPORT.md) · [질문 목록](docs/prd/OPEN_QUESTIONS.md) · [AWS 비용](docs/prd/AWS_COST.md)

이전 설계 참고: https://claude.ai/code/artifact/5a17973e-3162-4919-8d1a-c5015613303b
