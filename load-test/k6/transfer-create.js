// load-test/k6/transfer-create.js
// [업무 구현 후 실행] 이체 생성(POST /api/v1/transfers, MVP1-F15) 부하 시나리오.
// 현재 스켈레톤은 501을 반환한다 — 실행하면 not_implemented로만 집계되고
// 성공으로 기록되지 않는다. 가짜 성공 결과를 만들지 않기 위한 장치다.
//
// 변형(variant):
//   기본          — VU마다 서로 다른 출금 계좌(계좌 풀 라운드로빈)
//   VARIANT=contention — 모든 VU가 동일 출금 계좌(CONTENTION_ACCOUNT_ID)로 경합.
//                        확정 결정 "원장 비관적 잠금(동일 계좌 동시 출금)" 검증용.
//                        잔액 불변식 검증은 SCENARIOS.md §1 절차대로 별도 수행.
//
// 멱등키: Idempotency-Key 헤더(헤더명은 최종 계약 대조 필요 — 확인 필요).
//
// 실행: ./run.sh transfer-create.js
//       VARIANT=contention CONTENTION_ACCOUNT_ID=acc-000001 ./run.sh transfer-create.js

import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { baseUrl, classifyBusiness, idempotencyKey, jsonHeaders, LIMITS } from './lib/config.js';

const URL = `${baseUrl('transfer')}/api/v1/transfers`;
const VARIANT = __ENV.VARIANT || 'spread';
const RPS = Number(__ENV.RPS || 30);
const DURATION = __ENV.DURATION || '2m';
const AMOUNT = Number(__ENV.AMOUNT || 1000);

// 계좌 풀: 실제 시드 데이터 계정 ID로 교체 (확인 필요 — 업무 구현 후 시드 스크립트와 맞출 것)
const ACCOUNT_POOL = (__ENV.ACCOUNT_POOL ||
  'acc-000001,acc-000002,acc-000003,acc-000004,acc-000005,acc-000006,acc-000007,acc-000008')
  .split(',');
const CONTENTION_ACCOUNT_ID = __ENV.CONTENTION_ACCOUNT_ID || ACCOUNT_POOL[0];

const bizOk = new Counter('transfer_ok');
const bizNotImplemented = new Counter('transfer_not_implemented');
const bizConflict = new Counter('transfer_conflict'); // 409: 잔액 부족/중복 등 업무 거절
const bizError = new Counter('transfer_error');
const bizLatency = new Trend('transfer_duration', true);

export const options = {
  scenarios: {
    transfer_create: {
      executor: 'constant-arrival-rate',
      rate: RPS,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: Math.max(10, Math.ceil(RPS / 2)),
      maxVUs: Math.max(100, RPS * 4),
    },
  },
  thresholds: {
    // 501(미구현)은 http_req_failed에 잡히지 않으므로 별도 카운터로 감시한다.
    // 업무 구현 후에는 transfer_error와 http_req_failed가 목표율 이내여야 한다.
    transfer_error: ['count==0'],
    http_req_failed: [`rate<${LIMITS.bizErrorRate}`],
    transfer_duration: [`p(95)<${LIMITS.bizP95Ms}`, `p(99)<${LIMITS.bizP99Ms}`],
  },
};

function pickAccounts() {
  if (VARIANT === 'contention') {
    const debtor = CONTENTION_ACCOUNT_ID;
    const creditor = ACCOUNT_POOL.find((a) => a !== debtor) || ACCOUNT_POOL[1];
    return [debtor, creditor];
  }
  const debtor = ACCOUNT_POOL[__ITER % ACCOUNT_POOL.length];
  const creditor = ACCOUNT_POOL[(__ITER + 1) % ACCOUNT_POOL.length];
  return [debtor, creditor];
}

export default function () {
  const [debtor, creditor] = pickAccounts();
  // 요청 본문 필드명은 최종 계약 전 초안 — TransferController 구현과 대조할 것 (확인 필요)
  const payload = JSON.stringify({
    debtorAccountId: debtor,
    creditorAccountId: creditor,
    amount: AMOUNT,
    currency: 'KRW',
    memo: `loadtest-${VARIANT}`,
  });

  const res = http.post(URL, payload, {
    headers: jsonHeaders({ 'Idempotency-Key': idempotencyKey('transfer') }),
    timeout: LIMITS.reqTimeout,
    tags: { endpoint: 'transfer_create', variant: VARIANT },
  });

  const cls = classifyBusiness(res);
  if (cls === 'ok') bizOk.add(1);
  else if (cls === 'not_implemented') bizNotImplemented.add(1);
  else if (cls === 'conflict') bizConflict.add(1);
  else bizError.add(1);

  check(res, {
    'business 2xx (구현 후에만 통과)': () => cls === 'ok',
  });
  bizLatency.add(res.timings.duration, { variant: VARIANT });
}

export function handleSummary(data) {
  const suffix = VARIANT === 'contention' ? 'contention' : 'spread';
  const out = {};
  out[`results/transfer-create-${suffix}-summary.json`] = JSON.stringify(data, null, 2);
  return out;
}
