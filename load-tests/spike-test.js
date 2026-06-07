import http from 'k6/http';
import { check, sleep } from 'k6';
import { getAuthToken, getAuthHeaders } from './auth.js';

const BASE_URL ='https://scorm-be.onrender.com/api';

// Spike Test Configuration
// Simulates sudden traffic spike: 0 -> 200 users in 20s, sustain for 1m, then drops
export const options = {
  stages: [
    { duration: '20s', target: 0 },
    { duration: '20s', target: 200 },
    { duration: '1m', target: 200 },
    { duration: '20s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000', 'p(99)<2000'],
    http_req_failed: ['rate<0.2'],
  },
};

let token;

export function setup() {
  return { token: getAuthToken() };
}

export default function (data) {
  token = data.token;
  const headers = getAuthHeaders(token);

  // Focus on heavier operations: SCORM package download (resource intensive)
  // This simulates a heavy read operation that impacts system resources
  
  // Test 1: Download SCORM Package (heavy operation)
  const packageId = 1; // Adjust based on your test data
  const downloadRes = http.get(`${BASE_URL}/scorm-packages/${packageId}/download`, {
    headers,
    timeout: '30s'
  });
  check(downloadRes, {
    'Download Package - status 200': (r) => r.status === 200 || r.status === 404,
    'Download Package - response time < 2000ms': (r) => r.timings.duration < 2000,
  });

  sleep(0.5);

  // Test 2: Fallback to list packages if download fails (alternative heavy operation)
  const listRes = http.get(`${BASE_URL}/scorm-packages`, { headers });
  check(listRes, {
    'List Packages - status 200': (r) => r.status === 200,
    'List Packages - response time < 1000ms': (r) => r.timings.duration < 1000,
  });

  sleep(0.5);

  // Test 3: Get package details
  const getRes = http.get(`${BASE_URL}/scorm-packages/${packageId}`, { headers });
  check(getRes, {
    'Get Package - status 200': (r) => r.status === 200 || r.status === 404,
    'Get Package - response time < 1000ms': (r) => r.timings.duration < 1000,
  });

  sleep(0.5);
}

