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

Response: `CourseDetailResponse` (nested)

Ví dụ response:

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
  "courseUserId": 10,
  "thumbnail": {
    "courseId": 1,
    "imageMediaId": 123
  },
  "sections": [
    {
      "sectionId": 1,
      "title": "Section 1",
      "description": "Intro",
      "orderIndex": 1,
      "learningObjective": "Understand basics",
      "themeOverride": { "tokens": { "primary": "#111" } },
      "pages": [
        {
          "pageId": 1,
          "title": "Page 1",
          "orderIndex": 1,
          "pageType": "CONTENT",
          "themeOverride": { "tokens": { "fontSize": 14 } },
          "contentPage": {
            "pageId": 1,
            "layoutType": "SINGLE_COLUMN",
            "blocks": [
              {
                "blockId": 10,
                "orderIndex": 1,
                "textHtml": "<p>Hello</p>",
                "contentPageId": 1
              }
            ]
          },
          "quizPage": null
        },
        {
          "pageId": 2,
          "title": "Quiz 1",
          "orderIndex": 2,
          "pageType": "QUIZ",
          "themeOverride": null,
          "contentPage": null,
          "quizPage": {
            "pageId": 2,
            "passingScore": 80.0,
            "attemptAllowed": 3
          }
        }
      ]
    }
  ]
}
```

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
