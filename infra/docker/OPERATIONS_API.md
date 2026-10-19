# MVP1 한 번에 검증하는 로컬 REST API

Docker Desktop을 실행한 뒤 저장소 루트에서 아래 서버를 켠다. Python 3.9+ 표준 라이브러리만 사용하며 별도 패키지 설치는 없다.

```sh
python3 infra/docker/ops_api.py
```

기본 주소는 `http://127.0.0.1:38096`이다. 고객 Gateway의 `38080`과 별개다.
매 실행은 최신 로컬 소스로 이미지를 빌드하고 별도 `pg_mvp1_*` Docker 스택에서
인증·계좌·원장·이체·OTP·권한·Outbox·실제 Kafka 전달을 검증한 뒤 해당 스택/볼륨을 정리한다.
기존 `sinhan_project_docker` 스택을 이 테스트의 금융 요청 대상으로 사용하지 않는다.
초기 잔액만 테스트 fixture로 준비하고 이후 처리는 실제 서비스 REST 경로를 이용한다.

## GET / POST 계약

| 요청 | 의미 | 정상 응답 |
| --- | --- | --- |
| `POST /ops/v1/performance-runs` | `{"suite":"MVP1"}`로 검증 실행 접수 | `202 Accepted`, runId, Location |
| `GET /ops/v1/performance-runs/{runId}` | 실행 단계·상태·최종 결과 조회 | `200 OK` |
| `GET /ops/v1/performance-runs` | 최근 실행 목록, 최신 50건 | `200 OK` |

`/v1`은 요청/결과 형식의 버전이다. MVP1의 `1`과는 다른 의미이며 추후 형식 변경에 대비해 유지한다.
URI는 요청한 `performance-runs`를 쓰지만 현재 `kind=FUNCTIONAL_ACCEPTANCE`다.
TPS·동시 사용자·지연 백분위·일 천만 건 처리 능력을 측정하는 부하 테스트는 아직 연결하지 않았다.

모든 요청은 **전용 운영 Bearer 토큰**을 요구한다. 고객 로그인 JWT는 사용할 수 없다.
토큰과 실행 기록은 저장소 외부의 `/private/tmp/pg-platform-ops-<사용자 UID>`
(Linux에서는 `/tmp/pg-platform-ops-<UID>`)에 생성한다. 디렉터리는 700, 토큰은 600 권한이다.
API 응답에는 토큰, OTP 코드, DB 비밀번호, raw 로그를 포함하지 않는다.
원본 검증 증거와 runner 로그는 이 비공개 디렉터리에 남긴다.

```sh
# 두 번째 터미널에서: 토큰을 출력하지 않고 요청에 사용한다.
ops_state="${TMPDIR_OVERRIDE:-/private/tmp}/pg-platform-ops-$(id -u)"
# Linux는 위 /private/tmp를 /tmp로 바꾸거나 서버와 같은 --state-dir을 사용한다.
ops_token="$(cat "$ops_state/ops-token")"

# 새 검증: 오래 걸리는 작업이므로 즉시 202를 받고 GET으로 조회한다.
curl -sS -X POST http://127.0.0.1:38096/ops/v1/performance-runs \
  -H "Authorization: Bearer $ops_token" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: mvp1-my-first-run' \
  -d '{"suite":"MVP1"}'

# 응답의 runId를 넣는다.
curl -sS http://127.0.0.1:38096/ops/v1/performance-runs/<runId> \
  -H "Authorization: Bearer $ops_token"

curl -sS http://127.0.0.1:38096/ops/v1/performance-runs \
  -H "Authorization: Bearer $ops_token"
```

`Idempotency-Key`는 선택 항목이다. 같은 키로 POST를 재시도하면 기존 실행을 `200`으로 돌려준다.
**다음 검증을 새로 실행할 때는 새 키를 사용하거나 키를 생략한다.**
키를 재사용해도 과거 테스트가 다시 실행되지는 않는다. 단일 suite만 허용하므로 키별 body 변경은 지원하지 않는다.
서버 실행 중에는 소스/스키마를 수정하지 않고 해당 실행이 끝난 후 변경한다.

완료 예시(검사 개수는 suite 변경에 따라 달라질 수 있다):

```json
{
  "runId": "실제 UUID",
  "suite": "MVP1",
  "kind": "FUNCTIONAL_ACCEPTANCE",
  "status": "SUCCEEDED",
  "phase": "FINISHED",
  "summary": {"total": 65, "passed": 65, "failed": 0},
  "cleanupComplete": true,
  "existingStackPreserved": true
}
```

실제 응답에는 실행 시각, 소스 해시, 각 검사 이름과 통과 여부, self 링크도 포함한다.
단계는 `PREPARING → BUILDING → STARTING → VERIFYING → CLEANING → FINISHED`다.
`RUNNING`일 때 3초 정도 간격으로 GET을 반복하고 `SUCCEEDED`, `FAILED`, `INTERRUPTED`면 멈춘다.
GET의 200은 조회 성공이며, 테스트 통과 여부는 반드시 `status`로 판단한다.

| HTTP | 의미 |
| --- | --- |
| 400 | 지원하지 않는 suite/추가 필드/JSON/멱등키 |
| 401 | 운영 토큰 누락 또는 불일치 |
| 403 | 브라우저 Origin 요청 차단 |
| 404 | 없는 실행 또는 경로 |
| 405 | 허용하지 않는 HTTP 메서드 |
| 409 | 다른 실행이 진행 중. 응답 runId로 조회 |
| 413 / 415 | 본문 1KB 초과/빈 본문 또는 JSON Content-Type 누락 |

검증 실패·Docker 미실행·빌드 실패는 접수 후 `FAILED`로 기록한다. Docker API 접근 권한은 서버를 실행한 로컬 사용자 권한을 사용한다.
한 state 디렉터리에 한 서버/실행만 허용한다. 원격 bind나 CORS는 제공하지 않으며 Docker socket을 은행 서비스 컨테이너에 넣지 않는다.
일반 서비스 기능처럼 AWS에 배포하는 대상은 아니다. AWS용 운영 실행은 별도 실행 환경과 권한 설계가 필요하다.

## 종료·재시작·기록

Ctrl+C는 이미 접수한 검증의 종료/정리를 기다린다. 강제 종료하지 않는 편이 안전하다.
서버가 갑자기 종료되어도 이미 실행한 자식 검증은 계속되며, 해당 검증이 끝나기 전 같은 state 디렉터리의 서버 재시작은 거절한다.
그 뒤 재시작하면 저장된 evidence로 결과를 복원한다. evidence 없이 중단된 기록은 `INTERRUPTED`로 표시하며 금융 요청을 자동 재시도하지 않는다.
`cleanupComplete=false` 또는 정리 결과가 없는 실패는 비공개 runner 로그를 보고 해당 `pg_mvp1_*` 프로젝트만 확인/정리한 후 다음 실행을 한다.
고객 서비스용 Compose 프로젝트에 `down --volumes`를 실행하지 않는다.

다른 포트/장기 보존 위치가 필요하면:

```sh
python3 infra/docker/ops_api.py --port 38096 --state-dir /absolute/private/ops-state
```

`/private/tmp`는 OS가 지울 수 있으므로 장기 이력에는 외부 영구 경로를 선택한다.
기본 목록은 최근 50건이지만 저장된 과거 기록의 개별 GET은 계속 가능하다. 상태/로그의 자동 삭제는 하지 않는다.

HTTP 계약 회귀 검증:

```sh
python3 -m unittest discover -s infra/docker -p 'test_ops_api.py' -v
```

설계 근거: Figma **시스템 아키텍처 구성도 `19:2`**와 accepted ADR 0001.
이번 Figma 재조회는 MCP 한도 때문에 실패해 최신 도식의 추가 변경은 **확인 필요**다.
기존 Gateway 문서에서 보류하던 운영 API 중 **로컬 MVP1 검증**만 2026-10-19 사용자 요청으로 구현했다.
별도 loopback 운영 서버·정확한 HTTP 계약은 이번 구현 결정이며 Figma에서 확정한 내용이라고 주장하지 않는다.
서비스 overview·AWS 운영·실제 성능 부하 API는 보류 상태다.

## 실제 검증 결과 — 2026-10-19

HTTP 계약 테스트 9개와 실제 POST→GET Docker 검증 65개가 모두 통과했다.
새 실행 202, 같은 키 재요청 200, 다른 동시 실행 409, 토큰 누락 401을 실제 서버에서도 확인했다.
테스트 컨테이너·볼륨·네트워크 제거와 기존 스택 보존을 확인했다.
증거: [mvp1-ops-api-2026-10-19.json](evidence/mvp1-ops-api-2026-10-19.json).
