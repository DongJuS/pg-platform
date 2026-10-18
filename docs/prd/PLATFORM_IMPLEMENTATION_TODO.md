# Platform Feature Implementation Placeholders

## MVP2-F12 — Integrated Observability and Alerts

- Existing files: the Collector, Prometheus and Grafana in infra/observability/, and build-logic/src/main/shared-resources/platform-defaults.yml.
- TODO: business metric names and cardinality, Kafka lag, alerts on error rate, latency and reconciliation mismatches, and Runbook links.
- TODO: check the actual Loki/Tempo/Alertmanager setup, retention and permissions separately. Do not treat them as deployed based on their names alone.

## MVP7-F05 — Load Testing

- Implementation location: add transfer/payment/report/settlement scenarios and result records under load-test/.
- TODO: record average/peak RPS, p95/p99, error rate, balance invariants, Kafka lag, recovery time and infrastructure conditions separately.
- TODO: scenarios for single-account contention, duplicate requests, concurrent refunds, lost responses, delayed events and closing reruns.
- The current architecture mutation checks in load-test/ do not replace business load testing. Keep real LLM load separate from the Mock.

## CMN-F03 — Build and Deployment

- Existing implementation: Java build, Testcontainers and Terraform validate/mock in .github/workflows/build.yml.
- TODO: connect the deployment stage after real images, credentials, deploy/rollback, observability and service authentication are implemented.
- Keep paid-resource toggles disabled by default, and keep AWS unapplied. Kubernetes/Argo CD follow the existing phased plan.
