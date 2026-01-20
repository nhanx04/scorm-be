# ContentPage API

## Base

- Base URL: `/api`

## Data model (DB)

- Table: `content_page`
- PK/FK: `pageid` (FK -> `page.pageid`)
- Fields:
  - `layout_type` (VARCHAR(50))

## Endpoints

### 1) Create ContentPage for an existing Page

- Method: `POST`
- Path: `/api/pages/{pageId}/content-page`

#### Request

- Path param:
  - `pageId`: `Long`
- Body:
  ```json
  {
    "layoutType": "SINGLE_COLUMN"
  }
  ```

#### Response (200)

```json
{
  "pageId": 123,
  "layoutType": "SINGLE_COLUMN"
}
```

#### Error cases

- `400 BAD_REQUEST`
  - `pageId is required`
- `401 UNAUTHORIZED`
  - not logged in
- `403 FORBIDDEN`
  - `You do not have permission to access this page`
- `404 NOT_FOUND`
  - `Page not found`
- `409 CONFLICT`
  - `ContentPage already exists for this page`

#### Notes

- Endpoint này chỉ tạo subtype row `content_page` cho một `page` đã tồn tại.
- `pageid` được map bằng shared primary key (`@MapsId`).

---

### 2) Update ContentPage

- Method: `PATCH`
- Path: `/api/content-pages/{pageId}`

#### Request

- Path param:
  - `pageId`: `Long`
- Body (partial update):
  ```json
  {
    "layoutType": "TWO_COLUMN"
  }
  ```

#### Response (200)

```json
{
  "pageId": 123,
  "layoutType": "TWO_COLUMN"
}
```

#### Error cases

- `400 BAD_REQUEST`
  - `pageId is required`
- `401 UNAUTHORIZED`
  - not logged in
- `403 FORBIDDEN`
  - `You do not have permission to access this page`
- `404 NOT_FOUND`
  - `Page not found`
  - `ContentPage not found`

## Delete behavior

- Khi `DELETE /api/pages/{pageId}` (Page API) được gọi, row tương ứng trong `content_page` sẽ bị xóa theo (cascade/orphanRemoval từ entity `Page`).
