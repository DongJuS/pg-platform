# AI Gateway MVP3 전송 준비 구현·검증 체크리스트

이 파일명은 기존 진입점을 유지한다. `skeleton` 프로필의 전송 준비 경로는 실제 Java 정책·PostgreSQL/JDBC로 구현했다. Docker는 기존 `docker,skeleton` 조립을 사용한다. 기본/aws 프로필에 업무 API를 활성화하거나 고객 스택에 배포하지 않았다.

## 이번 범위

- [x] 인증된 내부 호출자 → 권한 확인 → 입력 검증 → 마스킹 → 잔존 검사 → 인젝션 정책 → 준비 판단 → 감사 commit → 내부 응답.
- [x] 호출자별 토큰을 기존 공통 내부 인증 필터의 opt-in 기능으로 검증한다. AI에서는 금융 공통 토큰으로 호출자 이름을 바꿀 수 없다. 금융 서비스의 기존 인증 계약은 유지한다.
- [x] 이름·주민번호·계좌번호·전화번호·이메일·카드번호를 아래 제한 안에서 마스킹한다. 동일 요청에서 같은 유형·정규화 값은 같은 토큰을 쓴다.
- [x] 복원 사전은 요청 중 지역 변수로만 사용하고 DB·응답·로그에 저장하지 않는다. 복원 API·Kafka·배치·외부 LLM client bean은 없다.
- [x] 실제 PostgreSQL 감사 저장을 마친 뒤만 정상 준비 결과를 반환한다. 검사 오류는 ERROR/503, 감사 실패는 AI-AUDIT_UNAVAILABLE/503이다.
- [x] 기존 TraceFilter·Actuator·Prometheus·구조화 로그·Collector 구성을 재사용한다. 업무 메트릭 태그는 고정 enum decision/reason뿐이다.
- [ ] 모델 호출·응답 필터·재시도·failover·실제 토큰 과금/쿼터·정책 콘솔·통계 API·MVP4 보고서/PDF는 범위 밖이다.

## Figma·사용자 지시·문서 대조

2026-10-20에 연결된 브라우저에서 현재 파일을 직접 읽었다. Figma MCP는 Starter 호출 한도에 걸렸으나 브라우저로 상세 화면을 확인했다.

| 근거 | 확인한 현재 내용 | 기존 코드/문서와 차이·적용 |
| --- | --- | --- |
| `1:15` mvp3_보안AI게이트웨이 | 내부 요청 원문 → PII 마스킹 → 마스킹 후 LLM 전달, 이름/주민번호/계좌 토큰 예시. 입력·출력 검사, 감사, 정책·한도 | 기존 RelayLlmRequest는 pseudonymizedPrompt를 받고 실제 마스킹/검사/감사는 미구현이었다. 이번에는 Gateway 원문 마스킹과 전송 전 검사만 구현했다. |
| `19:2` 시스템 아키텍처 구성도 | 내부 서비스 → AI Gateway는 VPC peering·서비스 토큰. 보고서 연결선은 “가명처리된 요청” | 호출자가 가명처리한 보고서 흐름과 Gateway 원문 입력 흐름을 구분한다. 이번 사용자 지시는 Gateway 마스킹을 포함하며 미래 보고서 client를 활성화하지 않는다. |
| `19:621` VPC(보안 AI 게이트 웨이) | 요청 수신/서비스 토큰 → PII 마스킹(주민·계좌·카드·전화, 토큰 치환/복원 키 분리) → 인젝션 탐지 → 외부 호출. 감사의 요청 해시·차단 사유·토큰 수, 원문 미저장 | 외부 호출 앞에서 경로를 끝낸다. 감사는 이번 범위에 필요한 신원·판정·유형별 마스킹 횟수만 저장한다. 실제 모델 토큰 수나 원문 해시는 저장하지 않는다. |
| `19:621`의 테스트용 별도 스키마/원문 저장, `1:15`의 마스킹본 저장 | 도식에 저장 표기가 있음 | 사용자의 원문·프롬프트 본문·복원 매핑 임의 저장 금지 지시를 우선한다. 원문과 마스킹본 모두 감사 DB에 저장하지 않는다. |
| `2:2` 00_전체아키텍처 | Wealth Report Agent → AI Security Gateway → 외부 LLM의 분리 | 서비스 경계를 유지하고 이번 활성 경로의 외부 호출을 등록하지 않는다. 도식의 전체 PostgreSQL 표기보다 사용자 확정 Account/Ledger MariaDB 결정을 우선한다. |
| 사용자 제공 `14:2` 링크 | 현재 브라우저에서 노드 선택 없이 `0:1` 전체 페이지로 돌아감 | 해당 node-id 자체의 상세는 확인 필요다. 열린 다른 프레임의 근거와 구분한다. |

[참조 Figma](https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-621). 로컬 ADR 0001의 독립 AI 출구·기본 차단과 금융 결정은 유지한다. 미래 LLM hostname/redirect/timeout 연결은 아직 미반영 항목이다. AWS/NAT는 만들거나 켜지 않았다. ADR의 과거 “금융 미구현” 설명은 현재 MVP1/2의 구현 증거보다 앞선 시점의 기록이다.

Notion의 [AI 경유 초안](https://app.notion.com/p/3faad9292a4981a2b0d5c94e371b082c), [PRD](https://app.notion.com/p/3faad9292a49813bacbef22a4476700a), [기능명세](https://app.notion.com/p/3fbad9292a498092bd8af323af928ca0), [코드 컨벤션](https://app.notion.com/p/3faad9292a49801c8deeda524464df1d)을 읽었다. API 경유/검사·모델 정책은 보류 초안이며 이번 준비 API의 확정 계약으로 간주하지 않았다. 첨부 PDF/unknown blocks 및 기획/REST DB 전체 항목·외부 AI 리뷰는 조회하지 않았다.

## 활성 내부 API

`POST /internal/v1/ai/preparations`, `Content-Type: application/json; charset=UTF-8`

호출자는 `X-Internal-Caller`와 그 호출자 전용 `X-Internal-Service-Token`을 보낸다. 필터가 검증하고 설정한 `callerServiceId` 요청 속성만 사용한다. 본문의 서비스명/모델/context/기타 미지원 필드는 400이다. 중복 인증 헤더·중복 JSON 키·추가 JSON 문서·숫자의 문자열 강제변환도 거절한다.

로컬 기본 허용 호출자는 `wealth-report`다. `AI_PREPARATION_WEALTH_TOKEN`은 필수이고 기본값은 비어 있어 인증이 닫힌다. `init-env.py`가 금융 공통 토큰과 다른 임의 자격증명을 새 private env 파일에 생성한다. 허용 호출자는 `AI_PREPARATION_ALLOWED_CALLERS`로 좁힐 수 있다. 새 호출자는 별도 credential binding과 allowlist를 모두 설정해야 한다. 이는 로컬 MVP 권한 구성이지 프로젝트 전체 운영 접근 범위의 확정이 아니다.

```json
{
  "inputFormat": "TEXT",
  "prompt": "홍길동 고객 900101-1234567 계좌 110-123-456789의 패턴을 설명해 주세요.",
  "sensitiveValues": [{"type": "NAME", "value": "홍길동"}]
}
```

`sensitiveValues`는 필수 배열이며 없으면 400, 대상이 없으면 빈 배열이다. 호출자가 알고 있는 이름·비정형 식별자는 반드시 목록에 넣는다. 타입은 NAME/RRN/ACCOUNT/PHONE/EMAIL/CARD만 지원한다. 값이 본문에 존재하지 않거나 다른 타입으로 중복 선언되면 거절한다. 별도의 복원 매핑이나 사용자·계좌 DB 조회를 받지 않는다.

```json
{
  "requestId": "서버가 발급한 UUID",
  "status": "READY_FOR_TRANSFER",
  "reason": "CHECKS_PASSED",
  "maskedPrompt": "[PII_NAME_1] 고객 [PII_RRN_1] 계좌 [PII_ACCOUNT_1]의 패턴을 설명해 주세요.",
  "maskedCounts": {"NAME": 1, "RRN": 1, "ACCOUNT": 1},
  "policyVersion": "preparation-rules-v1",
  "externalTransferPerformed": false
}
```

READY_FOR_TRANSFER는 **명시된 전송 전 검사가 끝나 감사까지 저장됨**을 뜻한다. 외부 전송·LLM 호출 성공·답변 생성·모든 위협 탐지 인증을 뜻하지 않는다. 미래 발신자는 이 응답을 전송 권한이나 위조 방지 증표로 간주해서는 안 된다. 실제 외부 발신 경계에서는 입력과 정책을 다시 결합해 검증해야 한다. 응답은 `Cache-Control: no-store`다.

| HTTP | 의미/안전한 reason 또는 code |
| --- | --- |
| 200 | READY_FOR_TRANSFER / CHECKS_PASSED |
| 400 | INVALID_INPUT, UNSUPPORTED_TEXT, RESERVED_TOKEN 또는 AI-INVALID_INPUT |
| 403 | 인증 거절 COMMON-INTERNAL-AUTH; 인증 후 권한 FORBIDDEN_CALLER; RESIDUAL_PII; PROMPT_INJECTION |
| 413 | INPUT_TOO_LARGE |
| 415 | UNSUPPORTED_FORMAT 또는 지원하지 않는 media type |
| 503 | ERROR/INSPECTION_UNAVAILABLE 또는 AI-AUDIT_UNAVAILABLE. maskedPrompt 없음 |

업무 차단은 BLOCKED와 고정 reason을 반환하고 원문/부분 마스킹본을 반환하지 않는다. 구문 오류·인증 실패·transport 상한 초과는 공통 code/message/traceId 형태이며 본문을 노출하지 않는다. 요청이 유스케이스에 도달한 뒤의 검증·권한 거절도 감사한다. 인증·JSON 파싱·transport 단계에서 거절된 요청은 업무 감사 행을 만들지 않으며 기존 HTTP 관측 대상이다. 형식적 요청/인증 정보에 대한 별도 보안 감사 정책은 운영 범위에서 확인 필요다.

요청마다 새 requestId·감사 행을 만든다. 금융 효과나 외부 호출이 없으므로 멱등키/중복 요청 병합은 제공하지 않는다.

## 검증 한계와 입력 예산

- 실제 body 상한 65,536 bytes를 읽기 전에/읽는 중에 집행하여 Content-Length 미제공·chunked 우회를 막는다. prompt는 UTF-8 16,384 bytes, 민감값 최대 64개·정규화 후 각 2~256자다. JSON 깊이 16, 문자열·숫자 파서 상한도 적용한다. 이 값은 자원 보호를 위한 구현 기본값이며 모델 토큰 예산이나 검증된 운영 SLO가 아니다.
- NFKC로 전각 문자 등을 정규화한 뒤 처리한다. 숨김 format 문자·지원하지 않는 제어 문자·미완성 surrogate·사용자가 넣은 PII 토큰 표기는 거절한다. 완전한 Unicode/인코딩 우회 탐지를 주장하지 않는다.
- 주민번호 형태, 국내 전화번호 형태(+82 일부 포함), 일반 이메일 형태, 계좌형 숫자열(10~16자리/하이픈 묶음), 카드형 숫자열(16~19자리/4자리 묶음)을 보수적으로 마스킹한다. 계좌·카드의 실제 발급 여부나 주민번호 진위를 확인하지 않는다. 길게 이어진 미분류 숫자열은 잔존 검사에서 차단한다.
- 이름은 `이름/성명/고객명/예금주: 한글이름` 패턴과 호출자 민감값 목록을 사용한다. 자유문 속 미신고 이름·주소·외국 식별자·미지원 번호 형식은 놓칠 수 있다. 긴 일반 식별자/금액은 오탐 마스킹·차단될 수 있다. dates/짧은 금액/비율의 정상 예시는 테스트하지만 모든 숫자의 문맥을 이해하지 않는다.
- 인젝션은 이전 지시 무시, 시스템 프롬프트/비밀 출력 요구, 일부 role delimiter·탈옥 표현의 유한한 한국어·영어 deny 규칙이다. 정규화한 원래 입력과 마스킹 결과를 모두 검사한다. 모든 언어·우회 인코딩·간접 인젝션을 완전히 탐지하지 않는다.
- 토큰은 요청 내 치환 표식이다. 복원 가능한 영구 가명 ID나 암호화 체계를 제공하지 않는다. 원문/매핑은 처리 중 메모리에 있으며 Java GC의 즉시 안전 삭제까지 보장하지 않는다. 원문 저장·복원 권한을 추가하지 않았다.

## DB·파일 범위

PostgreSQL `ai_gateway_db`의 V1은 변경하지 않았다. V2가 `ai_request_audits`와 시각 인덱스를 만든다. UUID PK, 고정 enum CHECK, 음수 건수 금지, 준비 성공↔CHECKS_PASSED와 ERROR↔검사 오류 제약을 적용한다. 저장 필드는 requestId/caller/decision/reason/유형별 건수/policyVersion/createdAt뿐이다. 원문·마스킹본·원문 해시·비밀·모델 토큰 사용량은 없다. 타 서비스 DB 조회/JOIN은 없다.

Controller/DTO → PrepareAiRequestUseCase/command/result → AiGatewayApplicationService → 순수 masking/inspection 정책과 PiiInspectionPort → PostgreSQL audit port/repository/adapter/entity/mapper를 연결했다. 감사 쓰기는 짧은 REQUIRES_NEW 트랜잭션이며 commit 오류까지 정상 응답을 막는다. 외부 호출·Kafka event·배치가 없는 유스케이스이므로 새 이벤트/배치를 만들지 않았다.

기존 `/internal/v1/ai/inference`, `/internal/v1/ai/requests/{requestId}`, `/internal/v1/ai/security-statistics`와 RelayLlmRequest/RelayLlmUseCase는 보류 초안이다. HTTP mapping과 Spring bean을 등록하지 않는다. LlmEndpointPolicy의 빈 allowlist·정확한 HTTPS hostname 검사는 보존했으며 외부 client에는 연결하지 않았다. DeferredAiUsagePolicyPlan(MVP3-F03)은 비활성이다. 실제 모델 토큰 수·사용량·쿼터와 이번 인증·byte 상한을 혼동하지 않는다.

## 실행·검증 증거

```sh
JAVA_HOME=<JDK21> ./gradlew build -Pintegration=true --rerun-tasks --no-daemon --offline
python3 infra/docker/run-mvp1-e2e.py --suite MVP3 --observability --output /private/tmp/mvp3-result.json
python3 -m unittest discover -s infra/docker -p 'test_ops_api.py' -v
```

[최종 검증 기록](../../../infra/docker/evidence/mvp3-progress-2026-10-20.json)에 실제 결과를 기록한다. 이전 MVP2의 285/137/9 통과 기록을 이번 결과로 재사용하지 않는다. 분리 Docker suite는 새 프로젝트·동적 localhost 포트·새 볼륨만 만들고 MVP1→MVP2→MVP3 순으로 검증하며 자기 자원만 정리한다. AI의 인터넷 egress를 제거한 구성에서 준비 API를 검증한다. 검사 의존성 오류는 실제 PostgreSQL을 쓰는 Spring 통합 테스트에서 주입하고, Docker에서는 실제 DB write 장애를 검증한다.

남은 확인: 현재 접근되지 않는 Figma 14:2의 식별, 운영 허용 호출자/비밀 회전·TLS, 탐지 정확도 목표·추가 유형/형식, 감사 보존 기간/열람 권한/삭제 운영, 향후 복원 요구. 이번 구현은 이를 확정하거나 raw retention을 추가하지 않는다. MVP2의 자동 결과 조회는 기존 기본 비활성 설정 그대로다.

### 최종 로컬 검증 (2026-10-20)

| 검증 | 실제 결과 |
| --- | --- |
| 전체 Java·실제 DB 회귀 | 최초 전체 강제 실행 325개 통과; 최종 변경 후 전체 build의 최신 보고서 326개, 실패/오류/skip 0. 최종 변경 대상 AI 테스트 47개 재실행 통과 |
| 분리 Docker | 182개 통과 = MVP1 69 + MVP2 68 + MVP3 45. 실제 PostgreSQL write 실패·chunked cap·동시 요청·Prometheus scrape 포함 |
| 운영 API | 9개 통과 |
| Prometheus alert rules | 2개 시나리오·7개 expression assertion 통과 |
| 외부 LLM | 테스트용 port 감시 0회, 실제 구성에는 LLM bean 없음, Docker AI 인터넷 출구 없음 |
| 보존·재현 | 최종 Docker 소스 해시가 현재 소스와 일치. 테스트 자원 정리 완료, 기존 컨테이너·MVP2 금융 Java/SQL·.gitignore 보존 |

검증 실패를 숨기지 않는다. 초기 중복 인증 헤더 시험에서 모호한 헤더 처리를 발견해 AI의 호출자별 인증 모드에서 중복 헤더를 거절하도록 수정했고, 이후 실제 DB와 Docker 시험을 통과했다. 로컬 sandbox가 Gradle 캐시/Docker/임시 HTTP 소켓 접근을 막은 실행은 접근 허용 환경에서 다시 실행했다. 최초 강제 빌드와 최종 변경 후 incremental build의 실행 범위는 evidence에 구분했다.
