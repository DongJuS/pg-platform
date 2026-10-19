# 부하시험 결과 기록 템플릿 (MVP7-F05)

각 실행은 이 파일을 복사해 `results/<YYYY-MM-DD>-<시나리오>.md`로 기록한다.
지표 범주는 섞어 쓰지 않고 아래 섹션별로 분리 기록한다.

---

## 시나리오: <이름> — <날짜>

### 1. 실행 조건

| 항목 | 값 |
| --- | --- |
| 시나리오 스크립트 | k6/<script>.js |
| 변형(VARIANT) | |
| 목표 RPS / 지속 시간 | |
| 대상 엔드포인트 | |
| k6 이미지 | grafana/k6:latest (digest: ) |
| 커밋/빌드 식별 | |

### 2. 처리량·지연 (k6)

| 지표 | 값 |
| --- | --- |
| 총 요청 수 | |
| 평균 RPS | |
| 피크 RPS | |
| p95 (ms) | |
| p99 (ms) | |
| max (ms) | |
| 오류율 (http_req_failed) | |
| 업무 거절(409) 건수 | |
| 미구현(501) 건수 | |

### 3. 잔액 불변식

| 항목 | 시험 전 | 시험 후 | 판정 |
| --- | --- | --- | --- |
| 대상 계좌 잔액 | | | |
| Σ성공 출금액 | | | |
| 불변식 위반 건수 | | | |

### 4. Kafka lag

| consumer group | 시험 중 최대 lag | 종료 시 lag | 비고 |
| --- | --- | --- | --- |
| (측정 방법: SCENARIOS.md §5 — observability 프로필의 kafka-exporter 사용) | | | |

### 5. 복구 시간 (장애 주입 시나리오만)

| 항목 | 값 |
| --- | --- |
| 주입 장애 | |
| 장애 시작/해제 시각 | |
| 지표 정상화 시각 | |
| 복구 시간 (초) | |

### 6. 인프라 조건

| 항목 | 값 |
| --- | --- |
| 호스트 | (CPU / RAM) |
| 컨테이너 mem_limit | (compose.yaml 기준) |
| 시험 중 컨테이너 메모리 피크 | (docker stats) |
| DB·Kafka·Redis 버전 | |
| 네트워크 모드 | host / compose |

### 7. 판정 및 메모

- 임계값 통과 여부:
- 관찰된 이상:
- 후속 조치:

---
---

# 채운 예시 (실측)

> ※ 이 예시는 **구 스택(`infra/docker-compose`, 180xx 포트, kafka-native 4.0.1, redis 7.4)** 실측이다.
> canonical 스택(`infra/docker`, 38080~, kafka 3.9.1, redis 7.2) 재측정은
> `2026-10-18-baseline-health-canonical.md`를 본다.

## 시나리오: baseline-health — 2026-10-18

### 1. 실행 조건

| 항목 | 값 |
| --- | --- |
| 시나리오 스크립트 | k6/baseline-health.js |
| 변형(VARIANT) | 해당 없음 (SERVICES=transfer) |
| 목표 RPS / 지속 시간 | 20 RPS / 30s (constant-arrival-rate) |
| 대상 엔드포인트 | GET http://host.docker.internal:18084/actuator/health |
| k6 이미지 | grafana/k6:latest (digest sha256:e66db15b8601…) |
| 커밋/빌드 식별 | 로컬 작업 트리 (미커밋), transfer-0.1.0-SNAPSHOT.jar |

### 2. 처리량·지연 (k6, 실측)

| 지표 | 값 |
| --- | --- |
| 총 요청 수 | 601 |
| 평균 RPS | 20.03 |
| 피크 RPS | 20 (constant-arrival-rate 고정) |
| p95 (ms) | 6.93 |
| p99 (ms) | 10.94 |
| max (ms) | 26.44 |
| 오류율 | 0% (601건 전부 200, status=UP) |
| 업무 거절(409) | 해당 없음 |
| 미구현(501) | 해당 없음 |

임계값(p95<500ms, p99<1000ms, 오류율<1%) 전부 통과.
원시 데이터: results/2026-10-18-baseline-health-legacy-summary.json

### 3. 잔액 불변식

해당 없음 — 헬스 기준선은 상태를 변경하지 않는다.

### 4. Kafka lag

미측정. 헬스 기준선은 메시지를 생산하지 않으며, compose에 Kafka lag
exporter가 없어 consumer group lag 수집 경로가 아직 없다 (확인 필요 —
SCENARIOS.md §5).

### 5. 복구 시간

해당 없음 (장애 주입 없음).

### 6. 인프라 조건 (실측)

| 항목 | 값 |
| --- | --- |
| 호스트 | Apple M5 Pro / 48 GB RAM, macOS (Docker Desktop) |
| 컨테이너 mem_limit | postgres 512m, mariadb 512m, redis 128m, kafka 512m, prometheus 192m, grafana 256m, otel-collector 192m, transfer 512m, account 512m |
| 시험 중 컨테이너 메모리 | transfer 271MiB/512MiB, kafka 184.8MiB/512MiB, postgres 32MiB/512MiB (docker stats 1회 스냅샷) |
| DB·Kafka·Redis 버전 | postgres 17-alpine, mariadb 11.8, redis 7.4-alpine, apache/kafka-native 4.0.1 |
| 네트워크 모드 | host (host.docker.internal 경유) |
| 기동 중 서비스 | 인프라 전부 + transfer(smoke), account(db-smoke). payment/settlement/wealth-report/api-gateway는 compose 서비스 없음 |

### 7. 판정 및 메모

- transfer /actuator/health는 20 RPS에서 오류 없이 p99 11ms 수준.
- **account(18082)는 측정 제외**: 컨테이너는 Up이나 jar 내부 logback
  NoClassDefFoundError로 HTTP 워커 스레드가 죽어 /actuator/health가
  응답하지 않는다(5s 타임아웃). 기존 빌드 산출물 결함으로 보이며 본 작업
  범위(기존 파일 수정 금지) 밖 — **확인 필요**.
- 업무 시나리오(transfer-create 등)는 스켈레톤 501 상태이므로 실측하지
  않았다. 실행하면 `*_not_implemented` 카운터로만 집계된다.
