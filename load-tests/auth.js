/**
 * JWT Authentication Helper
 * Handles login and token retrieval for load tests
 */
import http from 'k6/http';

const BASE_URL = 'https://scorm-be.onrender.com/api';

export function getAuthToken() {
  // Test credentials - adjust as needed for your test environment
  const loginPayload = {
    email: "test@example.com",
    password: "TestPassword123!"
  };

  const loginResponse = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify(loginPayload),
    {
      headers: {
        'Content-Type': 'application/json'
      }
    }
  );

  if (loginResponse.status !== 200) {
    throw new Error(`Authentication failed: ${loginResponse.status}`);
  }

  const token = loginResponse.json('token');
  if (!token) {
    throw new Error('No access token in response');
  }

  return token;
}

export function getAuthHeaders(token) {
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  };
}

