# Load Testing with k6

Simple load testing module for SCORM Package Generator using k6.

## Prerequisites

- k6 installed: https://k6.io/docs/getting-started/installation/
- Backend running on `http://localhost:8080` (or set `BASE_URL` env var)
- Test account credentials in database (or update `auth.js`)

## Setup

1. Update test credentials in `auth.js`:

   ```javascript
   const loginPayload = {
     email: "test@example.com",
     password: "TestPassword123!",
   };
   ```

2. Ensure backend is running and accessible

## Tests

### Stress Test (`stress-test.js`)

Gradually increases load from 10 to 120 concurrent users over 14 minutes to identify performance degradation.

**Stages:**

- 1m @ 10 users
- 2m @ 30 users
- 2m @ 60 users
- 2m @ 90 users
- 2m @ 120 users
- 5m @ 120 users (sustained)
- 2m ramp down to 0

**Endpoints tested:**

- GET `/courses` - List courses
- GET `/scorm-packages` - List packages
- GET `/courses/{id}` - Get course details
- GET `/scorm-packages/{id}` - Get package details

**Run:**

```bash
k6 run stress-test.js
```

### Spike Test (`spike-test.js`)

Simulates sudden traffic spike: 0 → 200 users in 20s, sustain for 1m, then drop. Tests system resilience.

**Stages:**

- 20s @ 0 users
- 20s @ 200 users (spike)
- 1m @ 200 users (sustained)
- 20s ramp down to 0

**Endpoints tested:**

- GET `/scorm-packages/{id}/download` - Download SCORM package (heavy operation)
- GET `/scorm-packages` - List packages
- GET `/scorm-packages/{id}` - Get package details

**Run:**

```bash
k6 run spike-test.js
```

## Configuration

### Authentication (`auth.js`)

- Automatically logs in and retrieves JWT token
- Token is reused for all requests in the test
- Update credentials to match your test environment

### Thresholds

Stress Test:

- 95th percentile response time < 500ms
- 99th percentile response time < 1000ms
- Failure rate < 10%

Spike Test:

- 95th percentile response time < 1000ms
- 99th percentile response time < 2000ms
- Failure rate < 20%

## Notes

- Adjust `courseId` and `packageId` in test files to match your test data
- Adjust test credentials in `auth.js` for your environment
- For production testing, add more endpoints and increase thresholds if needed
- Response time thresholds can be adjusted based on acceptable SLA
