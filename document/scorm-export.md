# SCORM Export / Package APIs

Base path: (theo code hiện tại) _không có prefix_ `/api`, các controller mapping trực tiếp theo path.

## 1) POST `/courses/{courseId}/scorm-packages`

Tạo SCORM package (generate file zip + persist DB).

- Quyền:
  - Chỉ **owner** của course mới được phép.
- Hành vi:
  - Backend sẽ tạo một file `.zip` tối thiểu (hiện tại gồm `imsmanifest.xml` + `index.html`).
  - Lưu record vào bảng `scorm_package`.
  - `theme_snapshot` được chụp từ `scorm_export_config.theme_config` (có thể `null`).
  - Nếu course chưa có `scorm_export_config` thì backend sẽ tạo record rỗng để liên kết.

Request body:

```json
{
  "packageName": "Course 1 - export v1",
  "packageType": "SCORM_2004"
}
```

- `packageName` (optional): nếu không gửi sẽ tự sinh.
- `packageType` (optional): mặc định `SCORM_2004`.

Response: `ScormPackageResponse`

```json
{
  "scormPackageId": 1,
  "packageName": "Course 1 - export v1",
  "packageType": "SCORM_2004",
  "zipFilePath": "C:\\path\\to\\scorm-exports\\scorm-1-<uuid>.zip",
  "themeSnapshot": { "tokens": { "primary": "#111" } },
  "packageCourseId": 1,
  "packageConfigId": 10,
  "packageUserId": 5
}
```

DB mapping:

- `scorm_package.package_name` = request.`packageName`
- `scorm_package.package_type` = request.`packageType` (default `SCORM_2004`)
- `scorm_package.zip_file_path` = path tuyệt đối của file zip (local)
- `scorm_package.theme_snapshot` = `scorm_export_config.theme_config`
- `scorm_package.package_courseid` = `{courseId}`
- `scorm_package.package_configid` = `scorm_export_config.scorm_configid`
- `scorm_package.package_userid` = user hiện tại

---

## 2) GET `/scorm-packages`

List các SCORM package của user hiện tại.

- Quyền:
  - Authenticated.

Response: `ScormPackageResponse[]`

---

## 3) GET `/scorm-packages/{packageId}`

Lấy chi tiết SCORM package theo id.

- Quyền:
  - Chỉ **owner** (người tạo package) mới được phép.

Response: `ScormPackageResponse`

---

## 4) GET `/scorm-packages/{packageId}/download`

Download file zip của SCORM package.

- Quyền:
  - Chỉ **owner** (người tạo package) mới được phép.

Response:

- `200 OK`
- `Content-Type: application/octet-stream`
- `Content-Disposition: attachment; filename="{packageName}.zip"`

---

## 5) DELETE `/scorm-packages/{packageId}`

Xoá SCORM package.

- Quyền:
  - Chỉ **owner** (người tạo package) mới được phép.
- Hành vi:
  - Xoá file zip local nếu còn tồn tại.
  - Xoá record DB trong `scorm_package`.

Response:

- `200 OK` (empty body)

---

## Notes

- Hiện tại việc generate zip là bản tối thiểu để đảm bảo API hoạt động end-to-end (persist + download). Khi bạn muốn export “đúng SCORM” theo course tree, mình sẽ cần thêm logic build `imsmanifest.xml`, activities/resources theo `section/page` và asset files.
