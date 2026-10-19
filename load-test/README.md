# load-test — MVP7-F05 부하시험

로컬 Docker 스택(AWS 대체 환경) 대상 부하시험 자산. k6 로컬 설치 불필요 —
모든 실행은 `grafana/k6` Docker 이미지를 사용한다.

## 구성

| 경로 | 설명 |
| --- | --- |
| `k6/lib/config.js` | 공통 설정. 엔드포인트·임계값은 전부 환경변수화. 로컬→AWS 전환 시 `<SERVICE>_BASE_URL`만 바꾸면 된다 |
| `k6/baseline-health.js` | `[지금 실행 가능]` actuator/health 기준선 (RPS·지연·오류율) |
| `k6/transfer-create.js` | `[업무 구현 후]` 이체 생성. `VARIANT=contention`으로 단일 계좌 경합 |
| `k6/payment-approve-refund.js` | `[업무 구현 후]` 결제 승인 + 동시 환불 |
| `k6/report-generate.js` | `[업무 구현 후]` 보고서 동기 생성 + 시간 예산(`REPORT_BUDGET_MS`) 판정 |
| `k6/settlement-close.js` | `[업무 구현 후]` 마감 조회 + 재실행(rerun) 멱등성 |
| `SCENARIOS.md` | 6개 필수 상황 카탈로그 (목적·주입·관찰 지표·판정 기준) |
| `results/TEMPLATE.md` | 결과 기록 양식 + baseline-health 실측 예시 |
| `run.sh` | k6 Docker 래퍼 |

## 사전 조건

- 인프라 기동: canonical 스택(`infra/docker/compose.yaml`, 프로젝트
  `sinhan_project_docker`). env 파일은 저장소 밖에 생성한다
  (`infra/docker/init-env.py` 또는 `.env.example` 참조, 예: `/private/tmp/sinhan-docker.env`).

  ```bash
  # 예: 기준선 측정에 필요한 그룹만 기동 (변수는 infra/docker/.env.example 기준:
  # POSTGRES_PASSWORD, MARIADB_ROOT_PASSWORD, APP_DB_PASSWORD, GRAFANA_ADMIN_PASSWORD)
  sh infra/docker/stack.sh /private/tmp/sinhan-docker.env \
    up -d --wait --wait-timeout 240 api-gateway auth account ledger transfer
  ```

- 업무 서비스는 compose의 `apps` 프로필 기준. 서비스 이미지가 빌드되어 기동 중이어야 한다
  (compose `--build` 또는 `scripts/build-images.sh` 산출물 + `IMAGE_REGISTRY`/`IMAGE_TAG`).
- host 모드는 dev-access forwarder(127.0.0.1:38080~38095 ↔ 컨테이너 8080~8095)를 경유한다.
- 첫 실행 시 `grafana/k6:latest` 이미지를 pull한다.

## 실행

```bash
cd load-test

# 기준선 (지금 가능)
./run.sh baseline-health.js
SERVICES=transfer,account BASE_RPS=50 DURATION=2m ./run.sh baseline-health.js

# 업무 경로 (구현 후 — 지금 실행하면 not_implemented로만 집계됨)
./run.sh transfer-create.js
VARIANT=contention CONTENTION_ACCOUNT_ID=acc-000001 ./run.sh transfer-create.js
./run.sh payment-approve-refund.js
./run.sh report-generate.js
./run.sh settlement-close.js

# compose 네트워크 안에서 서비스 이름으로 접근
TARGET_MODE=compose ./run.sh baseline-health.js

# AWS 등 다른 환경: 엔드포인트만 교체
TRANSFER_BASE_URL=https://alb.example.com ./run.sh transfer-create.js
```

결과 요약은 `results/*-summary.json`에 남고, 해석 기록은 `results/TEMPLATE.md`
양식을 복사해 `results/<날짜>-<시나리오>.md`로 작성한다.

## 주의

- 현재 업무 로직은 스켈레톤(501)이다. 업무 시나리오를 지금 실행해도
  `*_not_implemented` 카운터로만 집계되며 성공으로 기록되지 않는다.
- 스크립트 문법만 미리 검증하려면:
  `docker run --rm -v "$PWD/k6:/scripts" grafana/k6:latest inspect /scripts/<script>.js`
- 요청 본문 필드명·멱등키 헤더명은 최종 계약 전 초안이다. 업무 구현 시
  Controller와 대조해 스크립트를 맞출 것(각 스크립트 주석의 "확인 필요" 참조).
