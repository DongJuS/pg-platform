// load-test/k6/payment-approve-refund.js
// [업무 구현 후 실행] 결제 승인(POST /api/v1/payments, MVP2-F01) +
// 동시 환불(POST /api/v1/payments/{paymentId}/refunds, MVP2-F04) 시나리오.
// 현재 스켈레톤은 501 — not_implemented로만 집계된다.
//
// 흐름:
//   setup()   — 승인된 결제 PAYMENT_COUNT건을 만들어 paymentId 목록 확보.
//               501이면 "미구현"을 출력하고 시나리오를 중단한다(가짜 성공 방지).
//   default() — 모든 VU가 같은 paymentId들에 동시 환불을 건다.
//               동일 paymentId에 환불이 몰리도록 설계(동시 환불 경합 검증).
//
// 관찰: 환불 합계가 승인 금액을 초과하지 않는지(판정은 SCENARIOS.md §3 절차로
// GET /api/v1/payments/{paymentId}/refunds 조회 결과로 별도 검증).

import http from 'k6/http';
import { check, fail } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { baseUrl, classifyBusiness, idempotencyKey, jsonHeaders, LIMITS } from './lib/config.js';

const BASE = baseUrl('payment');
const PAYMENT_COUNT = Number(__ENV.PAYMENT_COUNT || 5);
const REFUND_RPS = Number(__ENV.REFUND_RPS || 20);
const DURATION = __ENV.DURATION || '1m';
const AMOUNT = Number(__ENV.AMOUNT || 10000);

const refundOk = new Counter('refund_ok');
const refundConflict = new Counter('refund_conflict'); // 이미 환불됨/한도 초과 등 업무 거절
const refundError = new Counter('refund_error');
const refundLatency = new Trend('refund_duration', true);

export const options = {
  scenarios: {
    concurrent_refunds: {
      executor: 'constant-arrival-rate',
      rate: REFUND_RPS,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: Math.max(10, REFUND_RPS),
      maxVUs: Math.max(100, REFUND_RPS * 4),
    },
  },
  thresholds: {
    refund_error: ['count==0'],
    http_req_failed: [`rate<${LIMITS.bizErrorRate}`],
    refund_duration: [`p(95)<${LIMITS.bizP95Ms}`, `p(99)<${LIMITS.bizP99Ms}`],
  },
};

export function setup() {
  const ids = [];
  for (let i = 0; i < PAYMENT_COUNT; i++) {
    // 요청 본문 필드명은 최종 계약 전 초안 — PaymentController 구현과 대조할 것 (확인 필요)
    const payload = JSON.stringify({
      orderId: `loadtest-order-${__ENV.RUN_ID || 'local'}-${i}`,
      amount: AMOUNT,
      currency: 'KRW',
      method: 'CARD',
    });
    const res = http.post(`${BASE}/api/v1/payments`, payload, {
      headers: jsonHeaders({ 'Idempotency-Key': idempotencyKey(`pay-setup-${i}`) }),
      timeout: LIMITS.reqTimeout,
    });
    const cls = classifyBusiness(res);
    if (cls === 'not_implemented') {
      return { notImplemented: true, paymentIds: [] };
    }
    if (cls !== 'ok') {
      fail(`결제 승인 setup 실패: status=${res.status} body=${res.body}`);
    }
    let id = null;
    try {
      id = JSON.parse(res.body).paymentId;
    } catch (e) {
      /* fallthrough */
    }
    if (!id) fail(`결제 승인 응답에 paymentId 없음: ${res.body}`);
    ids.push(id);
  }
  return { notImplemented: false, paymentIds: ids };
}

export default function (data) {
  if (data.notImplemented) {
    check(data, { 'payment 미구현(501) — 시나리오 스킵': () => false });
    return;
  }
  const paymentId = data.paymentIds[__ITER % data.paymentIds.length];
  // 환불 요청 본문 초안 — 전액 환불 가정, 부분 환불 경합은 VARIANT로 확장 가능
  const payload = JSON.stringify({ amount: AMOUNT, reason: 'loadtest-concurrent-refund' });

  const res = http.post(`${BASE}/api/v1/payments/${paymentId}/refunds`, payload, {
    headers: jsonHeaders({ 'Idempotency-Key': idempotencyKey('refund') }),
    timeout: LIMITS.reqTimeout,
    tags: { endpoint: 'payment_refund' },
  });

  const cls = classifyBusiness(res);
  if (cls === 'ok') refundOk.add(1);
  else if (cls === 'conflict') refundConflict.add(1);
  else refundError.add(1);

  check(res, {
    '환불 2xx 또는 업무 거절(409)': () => cls === 'ok' || cls === 'conflict',
  });
  refundLatency.add(res.timings.duration);
}

export function handleSummary(data) {
  return {
    'results/payment-approve-refund-summary.json': JSON.stringify(data, null, 2),
  };
}
