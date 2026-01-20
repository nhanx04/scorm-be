# ContentBlock API

## Base

- Base URL: `/api`

## Data model

- Table: `content_block`
  - `blockid` (PK)
  - `order_index` (INT)
  - `text_html` (TEXT)
  - `content_pageid` (FK -> `content_page.pageid`)

## Endpoints

### 1) Create block

- Method: `POST`
- Path: `/api/content-pages/{pageId}/blocks`

Request body (`ContentBlockCreateRequest`):

```json
{
  "orderIndex": 1,
  "textHtml": "<p>Hello</p>"
}
```

Response (`ContentBlockResponse`):

```json
{
  "blockId": 10,
  "orderIndex": 1,
  "textHtml": "<p>Hello</p>",
  "contentPageId": 5
}
```

### 2) List blocks by content page

- Method: `GET`
- Path: `/api/content-pages/{pageId}/blocks`

Response: `List<ContentBlockResponse>`

```json
[
  {
    "blockId": 10,
    "orderIndex": 1,
    "textHtml": "<p>Hello</p>",
    "contentPageId": 5
  }
]
```

### 3) Get block by id

- Method: `GET`
- Path: `/api/content-blocks/{blockId}`

Response: `ContentBlockResponse`

### 4) Update block

- Method: `PATCH`
- Path: `/api/content-blocks/{blockId}`

Request body (`ContentBlockUpdateRequest`) (partial allowed):

```json
{
  "orderIndex": 2,
  "textHtml": "<p>Updated</p>"
}
```

Response: `ContentBlockResponse`

### 5) Delete block

- Method: `DELETE`
- Path: `/api/content-blocks/{blockId}`

Response:

- Status: `204 No Content`

## Authorization

- Requires authentication.
- Ownership rule: current user must be owner of the course that contains the page:
  - `content_block -> content_page -> page -> section -> course -> user`

## Notes: chèn asset vào page/block

Hiện tại schema chưa có bảng liên kết asset với `page`/`content_block`, nên cách làm là nhúng trực tiếp vào `textHtml`.

Ví dụ:

- Image:

```html
<img src="https://.../image.png" />
```

- Audio:

```html
<audio controls src="https://.../audio.mp3"></audio>
```

- Document:

```html
<a href="https://.../file.pdf" target="_blank" rel="noopener">Download</a>
```

- YouTube:

```html
<iframe src="https://www.youtube.com/embed/VIDEO_ID"></iframe>
```
