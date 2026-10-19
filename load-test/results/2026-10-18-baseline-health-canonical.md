# baseline-health — canonical 스택 재측정 (2026-10-18, WO-2 실측)

대상 스택: `infra/docker/compose.yaml` (프로젝트 `sinhan_project_docker`), dev-access forwarder 포트 38080~38095.
구 스택(`infra/docker-compose`, 180xx) 실측은 `2026-10-18-baseline-health-legacy-summary.json`에 보존.

## 시나리오: baseline-health — 2026-10-18 (canonical)

### 1. 실행 조건

| 항목 | 값 |
| --- | --- |
| 시나리오 스크립트 | k6/baseline-health.js |
| 변형 | ① host 단일: `SERVICES=transfer`(기본), BASE_RPS=20, DURATION=1m ② host 다중: `SERVICES=transfer,account,ledger BASE_RPS=50 DURATION=2m` ③ compose: `TARGET_MODE=compose`(기본 transfer, 20 RPS, 1m) |
| 대상 엔드포인트 | ①② GET http://host.docker.internal:38084(·38082·38083)/actuator/health (dev-access 경유) ③ http://transfer:8084 (네트워크 `sinhan_project_docker_main`, internal) |
| k6 이미지 | grafana/k6:latest (digest sha256:e66db15b8601…) |
| 커밋/빌드 식별 | 로컬 작업 트리 (미커밋), 이미지 `sinhan_project/transfer:local` 등 (멀티스테이지 `infra/docker/Dockerfile`) |
| 기동 서비스 | api-gateway auth account ledger transfer + 의존 인프라(postgres, mariadb, redis, kafka, kafka-init, dev-access, otel-collector) |

### 2. 처리량·지연 (k6, 실측)

① host / transfer 단일 / 20 RPS × 1m:

| 지표 | 값 |
| --- | --- |
| 총 요청 수 | 1200 |
| 평균 RPS | 20.00 |
| p95 (ms) | 12.02 |
| p99 (ms) | threshold 평가 통과(1000 미만, max 17.72) |
| max (ms) | 17.72 |
| 오류율 (http_req_failed) | 0% (1200건 전부 200, status=UP) |

② host / transfer,account,ledger / 50 RPS × 2m:

| 지표 | 값 |
| --- | --- |
| 총 요청 수 | 6001 |
| 평균 RPS | 50.01 |
| p95 (ms) | 8.06 |
| p99 (ms) | threshold 평가 통과(1000 미만, max 33.48) |
| max (ms) | 33.48 |
| 오류율 | 0% (6001건 전부 200, status=UP) |

③ compose / transfer / 20 RPS × 1m (네트워크 경로 검증):

| 지표 | 값 |
| --- | --- |
| 총 요청 수 | 1200 |
| 평균 RPS | 20.00 |
| p95 (ms) | 9.59 |
| max (ms) | 15.94 |
| 오류율 | 0% |

임계값(p95<500ms, p99<1000ms, 오류율<1%) **3회 실행 모두 통과(thresholds ok=true)**.
원시 데이터: `2026-10-18-baseline-health-canonical-host-transfer.json`, `-host-multi.json`, `-compose-transfer.json`
(대표 summary: `2026-10-18-baseline-health-canonical-summary.json` = ② 사본).

### 3. 잔액 불변식

해당 없음 — 헬스 기준선은 상태를 변경하지 않는다.

### 4. Kafka lag

미측정. 헬스 기준선은 메시지를 생산하지 않는다. lag 관찰 경로는 WO-3의 kafka-exporter
(observability 프로필)로 확보됐으나, consumer가 skeleton 상태라 lag 시리즈는 비어 있는 것이 정상.

### 5. 복구 시간

해당 없음 (장애 주입 없음).

### 6. 인프라 조건 (실측)

| 항목 | 값 |
| --- | --- |
| 호스트 | Apple 실리콘, macOS (Docker Desktop), VM 총 메모리 8,318,844,928 B ≈ 7.75 GiB / 15 CPU |
| 컨테이너 mem_limit | 앱 각 384m, postgres 384m, mariadb 384m, redis 96m, kafka 768m, dev-access 64m |
| 시험 중 컨테이너 메모리 | transfer 274.4MiB/384MiB, auth 248.8MiB, account 261.8MiB, ledger 264.1MiB, api-gateway 264.1MiB, kafka 305.3MiB/768MiB, postgres 56.3MiB/384MiB, mariadb 96.9MiB/384MiB, redis 16.7MiB/96MiB (docker stats 1회 스냅샷) |
| DB·Kafka·Redis 버전 | postgres 17-alpine, mariadb 11.8, redis 7.2-alpine, apache/kafka 3.9.1 (KRaft) |
| 네트워크 모드 | host(①②, host.docker.internal → dev-access 380xx) / compose(③, `sinhan_project_docker_main` — internal:true이지만 컨테이너 attach 가능, 외부 egress 불필요) |

### 7. 판정 및 메모

- **PASS**: canonical 스택에서 host/compose 두 모드 모두 임계값 통과. 50 RPS × 3서비스에서도 오류 0.
- 구 스택 실측에서 지적된 account 5s 타임아웃(logback NoClassDefFoundError)은 canonical 이미지에서는 재현되지 않음 — account가 50 RPS 다중 실행에 포함돼 오류 0.
- host 모드는 dev-access forwarder(haproxy) 홉이 추가되지만 p95는 compose 모드와 동수준(8~12ms).
- 업무 시나리오(transfer-create 등)는 여전히 스켈레톤 501 — 실측하지 않음(실행 시 `*_not_implemented`로만 집계).
- p99 값 자체는 k6 summary JSON에 개별 키로 남지 않고 threshold 평가(ok=true)로만 확인됨. max가 17~34ms이므로 p99<1000ms는 여유 있게 충족.
