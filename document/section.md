# Section APIs

## POST `/courses/{courseId}/sections`

Tạo section trong course (chỉ owner của course).

Request body:

```json
{
  "title": "Section 1",
  "description": "Intro",
  "orderIndex": 1,
  "learningObjective": "Understand basics",
  "themeOverride": { "tokens": { "primary": "#111" } }
}
```

Response: `SectionResponse`

## GET `/courses/{courseId}/sections`

List section trong course (chỉ owner của course).

Response: `SectionResponse[]`

## GET `/sections/{sectionId}`

Lấy chi tiết section theo id (chỉ owner).

Response: `SectionResponse`

## PATCH `/sections/{sectionId}`

Update section (chỉ owner).

Request body:

```json
{
  "title": "Section 1 updated",
  "orderIndex": 2,
  "themeOverride": { "tokens": { "primary": "#222" } }
}
```

Response: `SectionResponse`

## DELETE `/sections/{sectionId}`

Xoá section (chỉ owner).

Response: `200 OK` (empty body)
