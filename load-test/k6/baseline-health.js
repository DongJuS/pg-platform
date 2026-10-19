// load-test/k6/baseline-health.js
// [지금 실행 가능] 서비스 actuator/health 기준선 부하.
// 업무 로직(501 스켈레톤)과 무관하게 인프라·스프링 부트 기동 상태의
// RPS·지연·오류율 기준선을 잡는다.
//
// 실행: ./run.sh baseline-health.js
// 대상 지정: SERVICES=transfer,account ./run.sh baseline-health.js
// 부하 조정: BASE_RPS=50 DURATION=2m ./run.sh baseline-health.js

import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { baseUrl, LIMITS } from './lib/config.js';

const SERVICES = (__ENV.SERVICES || 'transfer')
  .split(',')
  .map((s) => s.trim())
  .filter((s) => s.length > 0);

const BASE_RPS = Number(__ENV.BASE_RPS || 20);
const DURATION = __ENV.DURATION || '1m';

const healthDuration = new Trend('health_duration', true);
const healthErrors = new Rate('health_error_rate');
const healthByService = new Counter('health_requests_by_service');

export const options = {
  scenarios: {
    health_baseline: {
      executor: 'constant-arrival-rate',
      rate: BASE_RPS,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: Math.max(10, Math.ceil(BASE_RPS / 2)),
      maxVUs: Math.max(50, BASE_RPS * 2),
    },
  },
  thresholds: {
    http_req_failed: [`rate<${LIMITS.healthErrorRate}`],
    'http_req_duration{expected_response:true}': [
      `p(95)<${LIMITS.healthP95Ms}`,
      `p(99)<${LIMITS.healthP99Ms}`,
    ],
    health_error_rate: [`rate<${LIMITS.healthErrorRate}`],
  },
};

export default function () {
  const service = SERVICES[__ITER % SERVICES.length];
  const url = `${baseUrl(service)}/actuator/health`;

  const res = http.get(url, {
    timeout: LIMITS.reqTimeout,
    tags: { service, endpoint: 'health' },
  });

  const ok = check(res, {
    'health 200': (r) => r.status === 200,
    'status UP': (r) => {
      try {
        return JSON.parse(r.body).status === 'UP';
      } catch (e) {
        return false;
      }
    },
  });

  healthDuration.add(res.timings.duration, { service });
  healthErrors.add(!ok, { service });
  healthByService.add(1, { service });
}

export function handleSummary(data) {
  return {
    'results/baseline-health-summary.json': JSON.stringify(data, null, 2),
  };
}
