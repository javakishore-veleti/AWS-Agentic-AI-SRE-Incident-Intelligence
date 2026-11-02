import http from 'k6/http';
import { check } from 'k6';

// Run only against a local catalog whose business fault was turned on.
// Start: FAULT_FAIL_REQUESTS=true on product-catalog-service, then
//   k6 run -e BASE_URL=http://127.0.0.1:8081 -e TOKEN="$ADMIN_TOKEN" CICD/Local/k6/inc-003-healthy-process.js
// Health stays up at GET /actuator/health. Stop: set FAULT_FAIL_REQUESTS back to false and restart.
export const options = {
  vus: 4,
  duration: '20s',
};

export default function () {
  const response = http.get(`${__ENV.BASE_URL}/api/products`, {
    headers: { Authorization: `Bearer ${__ENV.TOKEN}` },
    timeout: '5s',
  });
  check(response, {
    'catalog answered': (result) => result.status === 200 || result.status === 500,
  });
}
