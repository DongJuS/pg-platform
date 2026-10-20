# Wealth Report MVP4 구현·검증 체크리스트

이 파일명은 기존 진입점을 유지한다. 기존 `WealthReportApplicationService`의 501 본문을 실제 동기 파이프라인으로 교체했다. **실제 외부 LLM 보고서 완료를 뜻하지 않는다.** 현재 AI Gateway에는 준비 API만 있고 추론·응답 검사 계약은 없다.

작업 기준: `main` `dc6f7b144f333b01917247066627ff00c7cb5d9c`에서 `codex/mvp4-wealth-report`로 분기했다. 기존 `.gitignore` 수정과 `build-logic/.kotlin/`, 고객 컨테이너·볼륨을 보존한다. AWS·NAT·유료 리소스·실제 LLM 전송은 수행하지 않는다. MVP2 자동 결과 조회 기본 비활성은 유지한다.

## 설계 근거와 차이

2026-10-20 Figma MCP는 Starter 호출 한도에 걸렸다. 연결된 Chrome의 **현재** 프레임을 직접 열어 확인했다.

| 프레임 | 현재 확인 내용 | 코드·문서 차이와 적용 범위 |
| --- | --- | --- |
| `29:2` 자산관리 보고서 | Report API → 가명처리 → AI 요청 → 응답 수치·토큰 검증 → 복원 → PDF/S3, 별도 비동기 스냅샷 소비 | 기존 본문·SQL·PDF가 미구현이었다. 동기 생성·검증·단계 기록을 구현했다. 원문·복원 매핑은 활성화하지 않았다. 개발 저장은 report PostgreSQL의 비공개 PDF이며 S3 adapter/Presigned URL은 미구현·확인 필요다. |
| `19:2` 시스템 아키텍처 구성도 | 서비스와 AI 경계, 보고서와 금융 서비스 분리 | 타 서비스 DB 접근 없이 원장 소유 REST에서 동일 읽기 자료를 수집한다. 원장의 추가 수집 API·gross 산식은 개발 후보이며 사용자 확인 전 `policy.confirmed=false`로 닫힌다. |
| `2:2` 00_전체아키텍처 | Wealth Report Agent → AI Security Gateway → 외부 LLM, 서비스별 독립 DB | DB 경계를 유지한다. 도식의 일괄 PostgreSQL 표기보다 사용자 확정 Account/Ledger MariaDB 결정을 우선한다. |
| `1:15` mvp3_보안AI게이트웨이 | 요청 마스킹, 입력·출력 검사, 감사 | 준비 완료와 응답 생성 완료를 분리한다. 숫자 검증만으로 Gateway의 응답 보안 검사를 대체했다고 주장하지 않는다. |
| `19:621` VPC(보안 AI 게이트 웨이) | 서비스 토큰, PII 마스킹, 인젝션 탐지, LLM 호출·응답 검사·감사 | 활성 `/internal/v1/ai/preparations` client와 미설정 추론 port를 구분했다. 도식의 테스트 스키마 원문/매핑 보관은 사용자 금지 지시보다 우선하지 않는다. |

Notion 프로젝트·PRD·기능명세·생성/상세/목록 API·코드 컨벤션을 보조 대조했다. API 페이지는 모두 초안이다. 생성 초안의 비동기/202 후보보다 ADR 0001 4-A와 이번 동기 완료 지시를 우선한다. 목록의 cursor 후보와 기존 코드의 page 후보 중 기존 page DTO를 개발 계약으로 유지했다. `14:2` 상세, Notion 첨부 PDF/unknown blocks·외부 AI 리뷰는 이번에 확인하지 않았다.

## 구현 체크리스트

- [x] F01: 기존 Gateway JWT 검증·외부 신원 헤더 제거 계약을 재사용한다. Wealth에는 신뢰한 `X-User-Id`가 전달된다. 서비스 포트 자체는 내부망 전용이다.
- [x] 생성·조회·목록·PDF의 사용자 범위를 검사한다. 다른 사용자 보고서는 404, 계좌 수집 소유권 위반은 403이다.
- [x] 기간, 명시적 기준 시점, 계좌 집합, 멱등키, 페이지/상태를 검증한다. JSON 미지원·중복 필드·추가 문서·16KiB 실제 입력·중복 신원/멱등 헤더도 거절한다.
- [x] 원장 소유 `POST /internal/v1/ledger/report-snapshots`를 추가했다. 인증된 wealth-report 호출자만 허용하며 Account REST로 소유권을 확인한다. 외부 호출은 원장 DB 트랜잭션 밖이다.
- [x] 원장 MariaDB의 한 REPEATABLE_READ 자료 집합에서 모든 계좌 잔액·기간 흐름을 읽는다. 과거 잔액은 같은 읽기의 현재 잔액에서 기준 이후 분개를 차감한다. lazy-provision 전 미사용 계좌는 소유권 확인 뒤 0으로 다룬다.
- [x] `asOf`, `capturedAt`, source read identity와 계좌별 원장 버전을 보고서 PostgreSQL에 고정한다. 다른 시점의 REST 잔액을 섞지 않는다. DB의 수집 시각은 JVM 시간대 영향을 받지 않는 epoch로 읽는다.
- [x] F08 개발 후보 산식: 선택 예금계좌 KRW 잔액 합계·계좌별 gross 입금/출금. 기간 `[from,to)`, 잔액은 `postedAt<=asOf`. 원 단위 `UNNECESSARY`로 소수 원·통화 혼합을 거절한다. FX·수익률·부채 포함 순자산은 만들지 않는다.
- [x] F07: 초안의 세 금액 문자열·통화·기준 시점·계산 버전을 정확히 대조한다. 설명은 제한 문자·길이·숫자/주요 통화 표기·토큰 형식을 거절한다. 이는 제한된 구조 검증이며 모든 한국어 수 표현·의미적 허위·PII를 탐지하는 보안 검사는 아니다. 실제 LLM 활성화에는 Gateway 응답 검사 계약이 추가로 필요하다.
- [x] 실제 한글 PDF 생성: 라이선스를 포함한 NanumGothic 임베딩, 금액 오른쪽 정렬 표, 폭 측정 줄바꿈·페이지 분할·쪽 번호.
- [x] 개발 비공개 저장: PDF bytea insert와 COMPLETED 변경을 같은 로컬 PostgreSQL 트랜잭션으로 commit한다. 저장 실패 시 완료/PDF가 부분 공개되지 않는다. 매 다운로드마다 소유권을 다시 검사한다.
- [x] F10: RESERVED/COLLECTING/CALCULATING/DRAFTING/VALIDATING/RENDERING/STORING/COMPLETED 단계와 실패 코드를 보존한다. 만료된 PROCESSING은 INTERRUPTED로 탐지한다. 재시도 API는 보류, 외부 호출 재개·재과금은 수행하지 않는다.
- [x] 사용자+멱등키 unique, 동일 사용자 진행 작업 unique, PostgreSQL 예약 잠금으로 서비스 전체 동시 실행 수를 제한한다. 동일 키·동일 완료 요청은 기존 결과, 진행/실패/상이한 요청은 409다.
- [x] 고정 worker 수와 대기열 없는 실행으로 단계/총시간 예산을 집행한다. 취소를 무시하는 의존성도 worker를 계속 점유하므로 무제한 대체 실행이 생기지 않는다. 늦은 저장은 상태·기한 조건으로 차단한다.
- [x] 개발 기본 전체 90s·초안 45s·PDF 15s·저장 15s, 원장 수집 최대 15s, DB 질의/트랜잭션 5s. Gateway 보고서 route는 100s이며 전체 예산을 90s 초과로 설정하면 기동을 거절한다. 운영 SLO나 부하 검증 완료 값이 아니다. DB/오류 기록의 짧은 정리 시간이 전체 시간에 추가될 수 있다.
- [x] F05/F09: 실제 `LedgerPostedEvent`/`TransferCompletedEvent`를 소비한다. 두 이벤트의 `transactionId`/`journalId`가 같은 금융 효과를 나타낸다. `topic+eventId` 전달 중복과 `transactionId` 효과 중복을 별도 unique로 보호한다. 역순·지연 사건은 도착 시각과 발생 시각을 보존하며 수치를 더하지 않는다.
- [x] 이벤트에 없는 금액·계좌·일별 수치를 추정하지 않는다. 이벤트 투영은 변경 사실 인덱스이고 보고서 수치의 소스는 원장 읽기다. 결제 관련 원장 분개도 ledger.posted로 관찰한다. 이벤트가 자동 보고서를 만들지 않는다.
- [x] Kafka 성공 commit 후에만 ack한다. 오류는 offset을 넘기지 않는다. 독성 메시지의 유한 재시도/DLQ 운영 계약은 확인 필요다.
- [x] Gateway 준비 client는 caller 전용 credential·활성 경로·준비 판정·외부 전송 없음만 확인한다. 금융 공통 토큰 fallback·재시도·초안 변환은 없다. 현재 보고서 생성 조립에서는 등록하지 않는다.
- [x] 미설정 실제 `AiReportDraftPort`는 `REPORT-AI_NOT_CONFIGURED`로 실패한다. 현재 준비 응답을 외부 전송 증표나 초안으로 사용하지 않는다.
- [x] `report-test`에서만 합성 설명을 제공하고 JSON/PDF에 TEST_DOUBLE 출처를 표시한다. 이 프로필과 aws/prod/production 동시 사용은 거절한다. source/DB/PDF는 실제 구현을 사용한다.
- [ ] 실제 LLM 제공사·모델·전송 승인, 추론 wire 계약과 출력 보안 검사 구현.
- [ ] 사용자 산식·대상 데이터·스냅샷 완결성/지연 정책 확정. 질문 미응답 상태이며 기본 생성은 `REPORT-POLICY_UNCONFIRMED`로 닫힌다.
- [ ] 운영 S3 비공개 저장/조회 adapter, 암호화·보존·삭제·운영 열람 정책, 운영 예산·용량·SLO 확정.
- [ ] F06 이름/계좌번호 토큰화·복원은 이번 aggregate-only 후보에서 수집하지 않는다. TokenMapping 존재는 영구 저장 승인/구현이 아니다. 관련 legacy port/adapter는 비활성이다.
- [ ] 이벤트만으로 완결된 일별 수치 스냅샷을 재구축하는 기능. 식별자 이벤트 인덱스를 그 기능의 완료로 표현하지 않는다.

## 개발 API 계약

| 경로 | 입력/출력 | 동작·오류 |
| --- | --- | --- |
| POST `/api/v1/wealth/reports` | from/to/asOf ISO UTC(최대 microsecond 정밀도), accountIds 1..20, 필수 Idempotency-Key(ASCII 1..128) → reportId/status/downloadUrl/completedAt/draftSource | 기간 최대 366일은 자원 제한 기본값. 201은 PDF commit 이후. 400 입력,401 인증,403 계좌 권한,409 중복,422 자료/초안 불일치,429 동시 생성 제한,503 정책/추론/저장 미설정·장애,504 시간 초과. |
| GET `/api/v1/wealth/reports/{reportId}` | status/asOf/stage/failureCode/draftSource/downloadUrl | 본인만. 미완료에는 다운로드 없음. 새 작업을 실행하지 않는다. |
| GET `/api/v1/wealth/reports` | createdAt 기간 [from,to), status, page(0..10000), size(1..100, 기본20) → reports/nextPage | 본인 것만, createdAt desc/id 순서. |
| GET `/api/v1/wealth/reports/{reportId}/pdf` | PDF bytes | 매번 본인 JWT 필요, no-store/nosniff/attachment. URL 자체는 권한 증표가 아니므로 만료 시각은 null이다. |

기본/local/aws에는 보고서 업무 bean을 등록하지 않는 기존 정책을 유지한다. 개발 실행은 `skeleton`, 선택적 Kafka는 `platform.report.events.enabled=true`. 산식 승인 플래그 `platform.report.policy.confirmed` 기본 false다. 이 플래그를 켠 사실은 AI 제공사 승인이나 외부 전송 권한을 뜻하지 않는다. 분리 시험만 `docker,skeleton,report-test`와 후보 정책을 명시적으로 켠다. 기존 고객 스택에는 배포하지 않았다.

## 파일 체크리스트 대조

`docs/prd/FEATURE_FILE_CHECKLIST.md`는 위치 체크리스트이며 완료 판정이 아니다. F01~03/F07~10의 활성 경로는 Controller/요청 DTO → input port/command/result → ApplicationService → 순수 model/policy → port → JDBC report Entity/Mapper·SQL/source/PDF/storage adapter다. 실제 스냅샷 write는 `AssetReportPersistenceAdapter.saveSnapshot`, 단계 이력은 `stage`, 이벤트 write는 `ReportSnapshotWriteSkeleton`에 있다. 기존 `AssetSnapshotPersistenceAdapter`/그 Entity·Mapper, `ReportProgressSkeleton`, `ProcessedEffectPersistenceAdapter`, `PseudonymizationSkeleton`, TokenMapping 계열과 S3 placeholder는 조립하지 않으며 그 파일의 존재를 구현 완료로 세지 않는다. 보류 F04도 비활성이다.

## 재현·검증

```sh
JAVA_HOME=<JDK21> ./gradlew build -Pintegration=true --rerun-tasks --no-daemon --offline
python3 infra/docker/run-mvp1-e2e.py --suite MVP4 --observability --output /private/tmp/mvp4-result.json
python3 -m unittest discover -s infra/docker -p 'test_ops_api.py' -v
```

새 Docker suite는 독립 프로젝트·동적 loopback 포트·새 볼륨만 사용한다. AI 인터넷 출구를 제거한다. MVP1→MVP2→MVP3→MVP4 순으로 검증하고 자신이 만든 자원만 정리한다. PDF 시각 검증은 실제 Java 렌더러의 단일·여러 페이지 출력을 Poppler로 렌더링해 한글·표 정렬·값·잘림을 확인한다.

검증 결과·실패 후 수정·현재 소스 해시·대역과 실제 연동의 구분은 `infra/docker/evidence/mvp4-progress-2026-10-20.json`에 기록한다. 과거 326/182/9 기록을 새 검증으로 재사용하지 않는다.

## 최종 로컬 검증 (2026-10-20)

| 검증 | 결과 |
| --- | --- |
| 현재 Java·실제 DB 보고서 | 353개, 실패/오류/skip 0. 앞선 전체 강제 실행 352개 통과 후 최종 변경 모듈 재실행·미변경 항목 up-to-date. |
| 분리 Docker | 221개 통과 = MVP1 69 + MVP2 68 + MVP3 45 + MVP4 39. 검증 시점 src 해시와 실행 소스 해시 일치. PR 훅에서 OFL.txt 줄 끝 공백 두 곳만 정리했으며 실행 코드·폰트 바이트는 동일하다. |
| 운영 API | 9개 통과. |
| 실제 PDF | 원장 분개 후 잔액 80000·출금 20000 저장 및 PDF 다운로드. 한글·표·단일/두 페이지 시각 확인. 설명만 TEST_DOUBLE. |
| 재시작 | 만료 DRAFTING 행을 시험용으로 만든 뒤 실제 컨테이너를 재시작해 INTERRUPTED 표시·단계 보존·추가 PDF 없음 확인. 실제 유료 추론 중 강제 종료 검증은 아니다. |
| 보존 | 기존 컨테이너·볼륨·로컬 변경 보존, 시험 자원만 정리. 실제 LLM 0회, AWS/NAT 생성·활성화 없음. |
