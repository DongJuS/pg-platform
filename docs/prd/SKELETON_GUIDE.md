# 서비스 내부 구현 스켈레톤

이번 스켈레톤은 서비스의 기능별 실제 파일·메서드·입출력 DTO·처리 TODO다. 기존 대표 유스케이스 틀을 구체화해 일반 서비스 11개에 유스케이스 53개, API 정의 41개를 뒀다. API 정의 중 36개는 skeleton 프로필에서만 매핑하고 5개는 보류 상태로 주석만 남겼다. Gateway와 Mock 4개의 라우팅·필터·기관 API·시나리오 파일도 별도로 뒀다.

## 바로 볼 파일

- [AuthController — 회원가입·로그인·갱신·로그아웃·OTP](../../main-vpc/core/auth/src/main/java/com/shinhan/pg/auth/adapter/in/web/AuthController.java)
- [TransferController — 이체 생성·상태·목록](../../main-vpc/core/transfer/src/main/java/com/shinhan/pg/transfer/adapter/in/web/TransferController.java)
- [전체 API 목록](API_SKELETON_INDEX.md)
- [기능 ID → 파일 체크리스트](FEATURE_FILE_CHECKLIST.md)

각 API 메서드 주석에는 기능 ID, HTTP 메서드/경로, 계약 상태, 인증 전제, 입력/출력, 정상·오류 후보, 미정, 출처를 적었다. ApplicationService 메서드에는 실제로 채울 처리 순서를 TODO로 적었다. 이는 계약·파일 작업 완료이며 금융 기능 완료를 뜻하지 않는다.

## 서비스별 범위

| 서비스 | 개발용 HTTP 경로 | 보류 API | 조립하는 유스케이스 | 파일 안내 |
| --- | ---: | ---: | ---: | --- |
| auth | 6 | 0 | 6 | [SKELETON.md](../../main-vpc/core/auth/SKELETON.md) |
| account | 4 | 0 | 4 | [SKELETON.md](../../main-vpc/core/account/SKELETON.md) |
| ledger | 4 | 0 | 5 | [SKELETON.md](../../main-vpc/core/ledger/SKELETON.md) |
| transfer | 5 | 0 | 7 | [SKELETON.md](../../main-vpc/core/transfer/SKELETON.md) |
| payment | 8 | 0 | 10 | [SKELETON.md](../../main-vpc/core/payment/SKELETON.md) |
| settlement | 2 | 0 | 3 | [SKELETON.md](../../main-vpc/core/settlement/SKELETON.md) |
| swift | 0 | 1 | 3 | [SKELETON.md](../../main-vpc/external/swift/SKELETON.md) |
| wealth-report | 3 | 1 | 5 | [SKELETON.md](../../main-vpc/info/wealth-report/SKELETON.md) |
| aml | 2 | 0 | 4 | [SKELETON.md](../../main-vpc/info/aml/SKELETON.md) |
| ai-gateway | 0 | 3 | 1 | [SKELETON.md](../../ai-vpc/ai-gateway/SKELETON.md) |
| fep-adapter | 2 | 0 | 5 | [SKELETON.md](../../fep-vpc/fep-adapter/SKELETON.md) |

## 파일을 채우는 위치

```text
<service>/src/main/java/com/shinhan/pg/<service>/
├─ adapter/in/web/
│  ├─ <업무>Controller.java      # HTTP 경로·신원·요청 변환·응답 후보
│  ├─ request/<기능>Request.java
│  ├─ response/<기능>Response.java
│  └─ <서비스>ErrorMapper.java  # 업무 오류 → HTTP 계약, 현재 TODO
├─ application/
│  ├─ command/<기능>Command.java
│  ├─ result/<기능>Result.java
│  ├─ port/in/<기능>UseCase.java
│  ├─ port/out/<모델>Repository.java
│  └─ service/<서비스>ApplicationService.java
├─ domain/
│  ├─ model/<업무모델>.java     # 필드·불변식 주석, 순수 Java
│  ├─ policy/<업무규칙>.java    # 검증/계산 메서드와 구현 TODO
│  └─ error/<서비스>ErrorCode.java
├─ adapter/out/persistence/
│  ├─ <모델>PersistenceAdapter.java
│  ├─ entity/<모델>Entity.java  # 필드 자리, ORM/컬럼 매핑은 미정
│  ├─ mapper/<모델>EntityMapper.java
│  └─ outbox/<서비스>OutboxStore.java
└─ config/<서비스>SkeletonConfiguration.java
```

조회용 readmodel과 Entity를 구분했다. User/Account/Journal/Posting/Transfer/Payment/Refund/Report/SwiftMessage/AmlCase/FepMessage/SettlementCycle 등의 업무 모델·Repository·Entity·Mapper 파일을 만들었다. JPA/JdbcClient/MyBatis와 물리 컬럼을 임의 확정하거나 업무 migration을 추가하지 않았다. 각 Repository/Mapper의 미구현 메서드를 채우면서 실제 데이터 계약을 확정한다.

## 결정 유지

- [확정 ADR](../adr/0001-financial-integration-decisions.md): 금융 대외 요청은 내부 REST → FEP, 원장 ledger.posted와 이체 transfer.completed 발행 책임 분리.
- 보고서는 PDF/S3 저장 완료 후 동기 응답한다. 과거 문서의 202 접수·비동기 큐를 확정으로 복사하지 않았다. 생성 단계·기준 시점·복구 작업은 유지한다.
- Outbox 폴링과 원장 비관적 잠금의 구현 위치·트랜잭션 조건을 주석으로 둔다. 실제 SQL·동시성·DB 원자성은 아직 구현하지 않았다.
- account/ledger는 MariaDB, 나머지 저장 서비스는 PostgreSQL. 기존 AI 전용 NAT 코드와 배포 순서를 유지한다. 이번 작업에서 AWS를 호출·배포하지 않았다.
- 확장 로그아웃, AI 쿼터·통계, SWIFT 운영 조회 등 보류 기능은 코드 주석/파일 자리만 있으며 매핑을 등록하지 않는다. AI 중계 역할은 확정이나 inference HTTP 세부 계약은 기존 보류 표기를 유지한다.

## 실행 상태와 검증

기본/local/aws 프로필에는 신규 업무 Controller·ApplicationService를 등록하지 않는다. skeleton 프로필에서 유효한 요청을 호출하면 FeatureNotImplementedException을 공통 웹 handler가 **501**로 변환한다. 형식 오류는 **400**이다. 200/201/202/204 등의 주석·annotation은 구현할 정상 계약의 초안이며 현재 금융 성공을 만들어내지 않는다.

인증·인가·Bean Validation·HTTP 성공/실패 상세·페이지네이션·스키마·원장 로직·외부 client·실제 ORM/SQL은 TODO다. request attribute는 신뢰한 인증 문맥을 연결할 자리이며 실제 JWT/서비스 인증을 구현한 것으로 간주하지 않는다. API 계약 테스트는 이 스켈레톤 단계의 형식·미구현 표시를 검증한다. 기능을 구현할 때 해당 검사를 실제 인수 조건 테스트로 교체한다.

전체 빌드 **69개 테스트 통과**, 실패/오류/skip 0. 개발용 경로 36개, 보류 API 5개의 비노출, 기본 프로필 비활성, HTTP 501/400, 53개 유스케이스 조립, R1~R7, 기존 PostgreSQL/Kafka/Redis Testcontainers 검사를 확인했다. 금융 정합성·실제 동시 출금·목표 처리량 검증은 아니다.

### 2026-10-18 스켈레톤 갭 점검

- 77개 기능 ID를 소스와 대조했다. 게이트웨이의 CMN-F02·MVP2-F10·MVP2-F11만 소스 주석에 없어 GatewayRoutesConfiguration·GatewayJwtFilter·GatewayRateLimitFilter·InternalPathBlockFilter에 기능 ID와 구현 범위·미정 항목을 추가했다(라우팅 동작 변경 없음). 나머지 74개는 소스에서 추적 가능하다.
- transfer의 최초 스캐폴드 잔재 `domain/TransferErrorCode`(공통 ErrorCode 구현 예시)가 `domain/error/TransferErrorCode`(업무 오류 초안)와 중복이고 참조가 없어 삭제했다.
- 체크리스트 참조 파일 147개·api-skeleton-files.json 544개 전부 실제 존재, 보류 5개 매핑 비활성, skeleton 프로필 활성 매핑 36개를 재확인했다.
- Figma 프레임은 인증 없이 접근할 수 없어 확인하지 못했다. 게이트웨이 헬스·메트릭 범위 등 Figma 의존 항목은 소스 주석에 'Figma 프레임 확인 필요'로 표기했다. ADR 확정 결정과 코드·문서의 일치는 로컬로 검증했다.

[검증 결과](verification-api-skeleton.json) · [빌드 기록](verification-api-skeleton-build.log) · [파일 목록](api-skeleton-files.json)
