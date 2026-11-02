import http from 'k6/http';
import { check } from 'k6';

// Run only against a local inventory service whose delay was set on purpose.
// Start: FAULT_DELAY_MS=400 on inventory-service, then
//   k6 run -e BASE_URL=http://127.0.0.1:8085 CICD/Local/k6/inc-002-delay.js
// Stop: set FAULT_DELAY_MS back to 0 and restart the service.
export const options = {
  vus: 4,
  duration: '20s',
};

export default function () {
  const response = http.post(
    `${__ENV.BASE_URL}/api/reservations`,
    JSON.stringify({
      orderRef: `k6-${__VU}-${__ITER}`,
      lines: [{ sku: 'HED-CARAFE', qty: 1 }],
    }),
    { headers: { 'Content-Type': 'application/json' }, timeout: '5s' },
  );
  check(response, {
    'inventory answered': (result) => result.status === 204 || result.status === 409 || result.status === 500,
  });
}
