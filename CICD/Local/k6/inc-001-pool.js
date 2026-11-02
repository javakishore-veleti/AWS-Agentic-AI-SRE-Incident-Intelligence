import http from 'k6/http';
import { check } from 'k6';

// Run only against a local catalog whose pool was reduced on purpose.
// Start: HIKARI_MAX_POOL=1 on product-catalog-service, then
//   k6 run -e BASE_URL=http://127.0.0.1:8081 -e TOKEN="$ADMIN_TOKEN" CICD/Local/k6/inc-001-pool.js
// Stop: set HIKARI_MAX_POOL back to 10 and restart the service.
export const options = {
  vus: 8,
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
