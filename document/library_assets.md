# Media Asset API

## Base

- **Base URL**: `http://localhost:8080/api`
- **Auth**: Tất cả API dưới đây yêu cầu header `Authorization: Bearer <token>`.

## R2 Storage Metadata

Hệ thống upload file lên Cloudflare R2 và lưu thông tin trong `media_asset.metadata`.

Ví dụ metadata trả về:

```json
{
  "provider": "CLOUDFLARE_R2",
  "bucket": "scorm-generator",
  "endpoint": "https://<accountId>.r2.cloudflarestorage.com",
  "publicBaseUrl": "https://pub-...r2.dev",
  "key": "images/<uuid>-file.png",
  "publicUrl": "https://pub-...r2.dev/images/<uuid>-file.png",
  "contentType": "image/png",
  "size": 12345,
  "originalFileName": "file.png"
}
```

---

## 1) Create Library

- **URL**: `POST /libraries`

### Request JSON

```json
{
  "libraryName": "My Library",
  "description": "Library for assets",
  "scopeType": "PRIVATE"
}
```

### Response JSON (200)

```json
{
  "libraryId": 1,
  "libraryName": "My Library",
  "description": "Library for assets",
  "scopeType": "PRIVATE",
  "updatedAt": "2026-01-18T10:00:00+07:00",
  "owner": {
    "userId": 1
  }
}
```

---

## 2) Upload Image

- **URL**: `POST /media/images/upload`
- **Content-Type**: `multipart/form-data`

### Form-data

- `file`: binary
- `libraryId`: number
- `title`: string (optional)
- `description`: string (optional)

### Response JSON (200)

```json
{
  "mediaId": 1,
  "title": "Cover",
  "description": "Course cover",
  "originalFileName": "cover.png",
  "mediaType": "IMAGE",
  "uploadedAt": "2026-01-18T10:00:00+07:00",
  "metadata": {
    "provider": "CLOUDFLARE_R2",
    "bucket": "scorm-generator",
    "key": "images/<uuid>-cover.png",
    "publicUrl": "https://pub-...r2.dev/images/<uuid>-cover.png"
  }
}
```

---

## 3) Upload Audio

- **URL**: `POST /media/audios/upload`
- **Content-Type**: `multipart/form-data`

### Form-data

- `file`: binary
- `libraryId`: number
- `title`: string (optional)
- `description`: string (optional)

### Response JSON (200)

```json
{
  "mediaId": 2,
  "title": "BGM",
  "description": "Audio",
  "originalFileName": "bgm.mp3",
  "mediaType": "AUDIO",
  "uploadedAt": "2026-01-18T10:00:00+07:00",
  "metadata": {
    "provider": "CLOUDFLARE_R2",
    "bucket": "scorm-generator",
    "key": "audios/<uuid>-bgm.mp3",
    "publicUrl": "https://pub-...r2.dev/audios/<uuid>-bgm.mp3"
  }
}
```

---

## 4) Upload Document

- **URL**: `POST /media/documents/upload`
- **Content-Type**: `multipart/form-data`

### Form-data

- `file`: binary
- `libraryId`: number
- `title`: string (optional)
- `description`: string (optional)

### Response JSON (200)

```json
{
  "mediaId": 3,
  "title": "Spec",
  "description": "PDF",
  "originalFileName": "spec.pdf",
  "mediaType": "DOCUMENT",
  "uploadedAt": "2026-01-18T10:00:00+07:00",
  "metadata": {
    "provider": "CLOUDFLARE_R2",
    "bucket": "scorm-generator",
    "key": "documents/<uuid>-spec.pdf",
    "publicUrl": "https://pub-...r2.dev/documents/<uuid>-spec.pdf"
  }
}
```

---

## 5) Create Video (YouTube only)

- **URL**: `POST /media/videos`
- **Content-Type**: `application/json`

### Request JSON

```json
{
  "title": "Intro",
  "description": "Youtube embedded",
  "libraryId": 1,
  "youtubeUrl": "https://www.youtube.com/watch?v=xxxx"
}
```

### Response JSON (200)

```json
{
  "mediaId": 4,
  "title": "Intro",
  "description": "Youtube embedded",
  "originalFileName": null,
  "mediaType": "VIDEO",
  "uploadedAt": "2026-01-18T10:00:00+07:00",
  "metadata": {
    "provider": "YOUTUBE",
    "youtubeUrl": "https://www.youtube.com/watch?v=xxxx"
  }
}
```
