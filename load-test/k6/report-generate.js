// load-test/k6/report-generate.js
// [업무 구현 후 실행] 보고서 동기 생성(POST /api/v1/wealth/reports, MVP4-F01) 시나리오.
// 확정 결정 "보고서 동기 응답" — 응답 시간이 시간 예산(REPORT_BUDGET_MS) 안에
// 들어오는지가 핵심 판정이다. 현재 스켈레톤은 501 — not_implemented로만 집계.

import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { baseUrl, classifyBusiness, idempotencyKey, jsonHeaders, LIMITS } from './lib/config.js';

const URL = `${baseUrl('wealth-report')}/api/v1/wealth/reports`;
const RPS = Number(__ENV.RPS || 2); // 동기 생성은 무거운 경로 가정 — 낮은 RPS 출발 (확인 필요)
const DURATION = __ENV.DURATION || '2m';

const reportOk = new Counter('report_ok');
const reportNotImplemented = new Counter('report_not_implemented');
const reportError = new Counter('report_error');
const reportOverBudget = new Counter('report_over_budget');
const reportLatency = new Trend('report_duration', true);

export const options = {
  scenarios: {
    report_generate: {
      executor: 'constant-arrival-rate',
      rate: RPS,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: Math.max(5, RPS * 2),
      maxVUs: Math.max(20, RPS * 8),
    },
  },
  thresholds: {
    report_error: ['count==0'],
    report_over_budget: ['count==0'],
    http_req_failed: [`rate<${LIMITS.bizErrorRate}`],
    report_duration: [
      `p(95)<${LIMITS.reportBudgetMs}`,
      `p(99)<${LIMITS.reportBudgetMs * 1.5}`,
    ],
  },
};

export default function () {
  // 요청 본문 필드명은 최종 계약 전 초안 — WealthReportController 구현과 대조할 것 (확인 필요)
  const payload = JSON.stringify({
    userId: `loadtest-user-${(__VU % 10) + 1}`,
    period: { from: '2025-01-01', to: '2025-12-31' },
    includeHoldings: true,
  });

  const res = http.post(URL, payload, {
    headers: jsonHeaders({ 'Idempotency-Key': idempotencyKey('report') }),
    // 동기 응답이므로 요청 타임아웃은 시간 예산보다 충분히 크게 둔다
    timeout: __ENV.REQ_TIMEOUT || '60s',
    tags: { endpoint: 'report_generate' },
  });

  const cls = classifyBusiness(res);
  if (cls === 'ok') reportOk.add(1);
  else if (cls === 'not_implemented') reportNotImplemented.add(1);
  else reportError.add(1);

  if (cls === 'ok' && res.timings.duration > LIMITS.reportBudgetMs) {
    reportOverBudget.add(1);
  }

  check(res, {
    '보고서 2xx (구현 후에만 통과)': () => cls === 'ok',
    '시간 예산 준수': (r) => r.timings.duration <= LIMITS.reportBudgetMs,
  });
  reportLatency.add(res.timings.duration);
}

export function handleSummary(data) {
  return {
    'results/report-generate-summary.json': JSON.stringify(data, null, 2),
  };
}
