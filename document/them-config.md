# Theme Config APIs

Base path: (theo code hiện tại) _không có prefix_ `/api`, các controller mapping trực tiếp theo path.

## 1) PUT `/courses/{courseId}/scorm-export-config/theme`

Lưu theme config (JSONB) cho course vào bảng `scorm_export_config.theme_config`.

- Quyền:
  - Chỉ **owner** của course mới được phép.
- Hành vi:
  - **Upsert**: nếu course chưa có record `scorm_export_config` thì tạo mới, sau đó cập nhật `theme_config`.

Request body:

```json
{
  "themeConfig": {
    "tokens": {
      "primary": "#111"
    }
  }
}
```

Response: trả về JSON theme vừa lưu.

```json
{
  "tokens": {
    "primary": "#111"
  }
}
```

DB mapping:

- `scorm_export_config.theme_config` = request.`themeConfig`

---

## 2) PUT `/sections/{sectionId}/theme`

Lưu theme override (JSONB) cho section vào bảng `section.theme_override`.

- Quyền:
  - Chỉ **owner** của course chứa section mới được phép.

Request body:

```json
{
  "themeConfig": {
    "tokens": {
      "primary": "#222"
    }
  }
}
```

Response: trả về JSON theme vừa lưu.

```json
{
  "tokens": {
    "primary": "#222"
  }
}
```

DB mapping:

- `section.theme_override` = request.`themeConfig`

---

## 3) PUT `/pages/{pageId}/theme`

Lưu theme override (JSONB) cho page vào bảng `page.theme_override`.

- Quyền:
  - Chỉ **owner** của course chứa page mới được phép.

Request body:

```json
{
  "themeConfig": {
    "tokens": {
      "fontSize": 14
    }
  }
}
```

Response: trả về JSON theme vừa lưu.

```json
{
  "tokens": {
    "fontSize": 14
  }
}
```

DB mapping:

- `page.theme_override` = request.`themeConfig`
