import http from 'k6/http';
import { check, sleep } from 'k6';
import { getAuthToken, getAuthHeaders } from './auth.js';

const BASE_URL = 'https://scorm-be.onrender.com/api';

// Stress Test Configuration
// Gradually increases load from 10 to 120 concurrent users over 14 minutes
export const options = {
  stages: [
    { duration: '1m', target: 10 },
    { duration: '2m', target: 30 },
    { duration: '2m', target: 60 },
    { duration: '2m', target: 90 },
    { duration: '2m', target: 120 },
    { duration: '5m', target: 120 },
    { duration: '2m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.1'],
  },
};

let token;

export function setup() {
  // Get token once during setup
  return { token: getAuthToken() };
}

export default function (data) {
  token = data.token;
  const headers = getAuthHeaders(token);

  // Test 1: List Courses (common read operation)
  const listCoursesRes = http.get(`${BASE_URL}/courses`, { headers });
  check(listCoursesRes, {
    'List Courses - status 200': (r) => r.status === 200,
    'List Courses - response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(0.5);

  // Test 2: List SCORM Packages (another read operation)
  const listPackagesRes = http.get(`${BASE_URL}/scorm-packages`, { headers });
  check(listPackagesRes, {
    'List Packages - status 200': (r) => r.status === 200,
    'List Packages - response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(0.5);

  // Test 3: Get Course by ID (single read, potential bottleneck under stress)
  const courseId = 1; // Adjust based on your test data
  const getCourseRes = http.get(`${BASE_URL}/courses/${courseId}`, { headers });
  check(getCourseRes, {
    'Get Course - status 200': (r) => r.status === 200 || r.status === 404,
    'Get Course - response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(0.5);

  // Test 4: Get SCORM Package by ID
  const packageId = 1; // Adjust based on your test data
  const getPackageRes = http.get(`${BASE_URL}/scorm-packages/${packageId}`, { headers });
  check(getPackageRes, {
    'Get Package - status 200': (r) => r.status === 200 || r.status === 404,
    'Get Package - response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);
}

