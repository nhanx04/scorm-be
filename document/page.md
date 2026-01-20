# Page APIs

## POST `/sections/{sectionId}/pages`

Tạo page trong section (chỉ owner).

Request body:

```json
{
  "title": "Page 1",
  "orderIndex": 1,
  "pageType": "CONTENT",
  "themeOverride": { "tokens": { "fontSize": 14 } }
}
```

Response: `PageResponse`

## PATCH `/pages/{pageId}`

Update page (chỉ owner).

Request body:

```json
{
  "title": "Page 1 updated",
  "orderIndex": 2,
  "pageType": "QUIZ"
}
```

Response: `PageResponse`

## DELETE `/pages/{pageId}`

Xoá page (chỉ owner).

Response: `200 OK` (empty body)
