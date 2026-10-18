# 플랫폼 기능 구현 자리

## MVP2-F12 — 통합 관측성·알림

- 기존 파일: infra/observability/의 Collector·Prometheus·Grafana, build-logic/src/main/shared-resources/platform-defaults.yml.
- TODO: 업무 메트릭 이름·카디널리티, Kafka lag, 오류율·지연·대사 불일치 alert, Runbook 연결.
- TODO: Loki/Tempo/Alertmanager 실제 구성과 보존·권한은 별도 확인. 이름만 보고 배포 완료로 간주하지 않는다.

## MVP7-F05 — 부하시험

- 구현 위치: load-test/에 이체/결제/보고서/정산 시나리오와 결과 기록 추가.
- TODO: 평균/피크 RPS·p95/p99·오류율·잔액 불변식·Kafka lag·복구 시간·인프라 조건을 분리 기록.
- TODO: 단일 계좌 경합·중복 요청·동시 환불·응답 유실·지연 이벤트·마감 재실행 시나리오.
- 현재 load-test/의 architecture mutation 검사는 업무 부하시험을 대체하지 않는다. 실제 LLM 부하는 Mock과 구분한다.

## CMN-F03 — 빌드·배포

- 기존 구현: .github/workflows/build.yml의 Java build·Testcontainers·Terraform validate/mock.
- TODO: 실제 이미지·인증정보·배포/롤백·관측성·서비스 인증 구현 뒤 배포 단계를 연결한다.
- 유료 리소스 토글 기본 비활성과 AWS 미적용을 유지한다. Kubernetes/Argo CD는 기존 단계적 계획을 따른다.
