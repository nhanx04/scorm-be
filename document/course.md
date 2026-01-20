# Course APIs

Base path: `/courses`

## POST `/courses`

Tạo course.

Request body:

```json
{
  "title": "Course 1",
  "passingScore": 80.5,
  "attemptLimit": 3,
  "durationMin": 45,
  "status": "DRAFT",
  "extraInfor": { "foo": "bar" }
}
```

Response:

```json
{
  "courseId": 1,
  "title": "Course 1",
  "passingScore": 80.5,
  "attemptLimit": 3,
  "durationMin": 45,
  "status": "DRAFT",
  "lastPublishedAt": null,
  "updatedAt": "2026-01-20T00:00:00Z",
  "extraInfor": { "foo": "bar" },
  "createdAt": "2026-01-20T00:00:00Z",
  "courseUserId": 10
}
```

## GET `/courses`

List course của user hiện tại.

Response: `CourseResponse[]`

## GET `/courses/{courseId}`

Lấy chi tiết course theo id (chỉ owner).

Response: `CourseResponse`

## PATCH `/courses/{courseId}`

Update meta course (chỉ owner). Các field nullable: chỉ update field được gửi lên.

Request body:

```json
{
  "title": "Course 1 updated",
  "status": "PUBLISHED",
  "extraInfor": { "a": 1 }
}
```

Response: `CourseResponse`

## DELETE `/courses/{courseId}`

Xoá course (chỉ owner).

Response: `200 OK` (empty body)
