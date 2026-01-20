# QuizPage API

## Base

- Base URL: `/api`

## Data model (DB)

- Table: `quiz_page`
- PK/FK: `pageid` (FK -> `page.pageid`)
- Fields:
  - `passing_score` (NUMERIC(5,2))
  - `attempt_allowed` (INT)

## Endpoints

### 1) Create QuizPage for an existing Page

- Method: `POST`
- Path: `/api/pages/{pageId}/quiz-page`

#### Request

- Path param:
  - `pageId`: `Long`
- Body:
  ```json
  {
    "passingScore": 80.0,
    "attemptAllowed": 3
  }
  ```

#### Response (200)

```json
{
  "pageId": 123,
  "passingScore": 80.0,
  "attemptAllowed": 3
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
  - `QuizPage already exists for this page`

#### Notes

- Endpoint này chỉ tạo subtype row `quiz_page` cho một `page` đã tồn tại.
- `pageid` được map bằng shared primary key (`@MapsId`).

---

### 2) Update QuizPage

- Method: `PATCH`
- Path: `/api/quiz-pages/{pageId}`

#### Request

- Path param:
  - `pageId`: `Long`
- Body (partial update):
  ```json
  {
    "passingScore": 90.0,
    "attemptAllowed": 2
  }
  ```

#### Response (200)

```json
{
  "pageId": 123,
  "passingScore": 90.0,
  "attemptAllowed": 2
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
  - `QuizPage not found`

## Delete behavior

- Khi `DELETE /api/pages/{pageId}` (Page API) được gọi, row tương ứng trong `quiz_page` sẽ bị xóa theo (cascade/orphanRemoval từ entity `Page`).
