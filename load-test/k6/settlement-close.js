// load-test/k6/settlement-close.js
// [업무 구현 후 실행] 정산 마감 조회 + 재실행(MVP7-F04) 시나리오.
//   GET  /internal/v1/settlements/{date}
//   POST /internal/v1/settlements/{date}/rerun
// 현재 스켈레톤은 501 — not_implemented로만 집계된다.
//
// 마감 재실행 검증:
//   같은 날짜에 rerun을 연속 2회 호출하고, 두 번째 호출이 멱등하게 처리되는지
//   (2xx 멱등 수용 또는 409 업무 거절 — 최종 계약에 따라 판정, 확인 필요) 본다.
//   재실행으로 집계가 이중 반영되지 않았는지는 SCENARIOS.md §6 절차대로
//   GET /internal/v1/settlements/{date} 스냅샷 전후 비교로 별도 검증한다.

import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { baseUrl, classifyBusiness, jsonHeaders, LIMITS } from './lib/config.js';

const BASE = baseUrl('settlement');
const SETTLEMENT_DATE = __ENV.SETTLEMENT_DATE || '2025-12-31';
const RERUN_RPS = Number(__ENV.RERUN_RPS || 1); // 마감은 저빈도 경로
const DURATION = __ENV.DURATION || '1m';

const closeOk = new Counter('settlement_close_ok');
const closeNotImplemented = new Counter('settlement_not_implemented');
const rerunOk = new Counter('settlement_rerun_ok');
const rerunConflict = new Counter('settlement_rerun_conflict');
const rerunError = new Counter('settlement_rerun_error');
const closeLatency = new Trend('settlement_close_duration', true);
const rerunLatency = new Trend('settlement_rerun_duration', true);

export const options = {
  scenarios: {
    close_and_rerun: {
      executor: 'constant-arrival-rate',
      rate: RERUN_RPS,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: 5,
      maxVUs: 10,
    },
  },
  thresholds: {
    settlement_rerun_error: ['count==0'],
    http_req_failed: [`rate<${LIMITS.bizErrorRate}`],
  },
};

export default function () {
  // 1) 마감 조회
  const getRes = http.get(`${BASE}/internal/v1/settlements/${SETTLEMENT_DATE}`, {
    timeout: LIMITS.reqTimeout,
    tags: { endpoint: 'settlement_get' },
  });
  const getCls = classifyBusiness(getRes);
  if (getCls === 'ok') closeOk.add(1);
  else if (getCls === 'not_implemented') closeNotImplemented.add(1);
  closeLatency.add(getRes.timings.duration);

  // 2) 같은 날짜 rerun 연속 2회 — 재실행 멱등성
  for (let attempt = 1; attempt <= 2; attempt++) {
    const res = http.post(`${BASE}/internal/v1/settlements/${SETTLEMENT_DATE}/rerun`, null, {
      headers: jsonHeaders(),
      timeout: __ENV.REQ_TIMEOUT || '60s',
      tags: { endpoint: 'settlement_rerun', attempt: `${attempt}` },
    });
    const cls = classifyBusiness(res);
    if (cls === 'ok') rerunOk.add(1);
    else if (cls === 'conflict') rerunConflict.add(1);
    else if (cls === 'not_implemented') closeNotImplemented.add(1);
    else rerunError.add(1);

    check(res, {
      'rerun 2xx 또는 멱등 거절(409)': () => cls === 'ok' || cls === 'conflict',
    });
    rerunLatency.add(res.timings.duration, { attempt: `${attempt}` });
  }
}

export function handleSummary(data) {
  return {
    'results/settlement-close-summary.json': JSON.stringify(data, null, 2),
  };
}
