# ThumbnailOfCourse API

## Base

- Base URL: `/api`

## Data model

- Table: `thumbnail_of_course`
  - `thumbnail_courseid` (PK, FK -> `course.courseid`)
  - `thumbnail_mediaid` (FK -> `image_asset.mediaid`)

Constraint:

- One course has at most one thumbnail (PK = courseId).

## Endpoints

### 1) Upsert course thumbnail

- Method: `PUT`
- Path: `/api/courses/{courseId}/thumbnail`

Request body (`ThumbnailOfCourseUpsertRequest`):

```json
{
  "imageMediaId": 123
}
```

Response (`ThumbnailOfCourseResponse`):

```json
{
  "courseId": 5,
  "imageMediaId": 123
}
```

Notes:

- `imageMediaId` phải là `mediaid` thuộc bảng `image_asset`.

### 2) Get course thumbnail

- Method: `GET`
- Path: `/api/courses/{courseId}/thumbnail`

Response (`ThumbnailOfCourseResponse`)

### 3) Delete course thumbnail

- Method: `DELETE`
- Path: `/api/courses/{courseId}/thumbnail`

Response:

- Status: `204 No Content`

## Authorization

- Requires authentication.
- Ownership rule: current user must be owner of the course (`course.course_userid`).

## Typical flow

1. Upload image via `POST /media/images/upload` to get `mediaId`.
2. Set thumbnail via `PUT /api/courses/{courseId}/thumbnail` with `imageMediaId` = uploaded image `mediaId`.
