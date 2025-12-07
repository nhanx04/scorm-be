# SCORM Package Generator - Backend API

REST API Backend để tạo gói SCORM 2004 từ câu hỏi và đáp án. Dự án này đã được chuyển đổi từ Desktop App sang Backend REST API sử dụng Spring Boot.

## 🚀 Chạy nhanh (Quick Start)

**Yêu cầu:** Java 11+ và Maven 3.6+ đã cài đặt

```bash
# 1. Build dự án
mvn clean install

# 2. Chạy ứng dụng
mvn spring-boot:run
```

API sẽ chạy tại: `http://localhost:8080/api`

**Yêu cầu:** Java 11+ đã cài đặt ([Tải Java](https://adoptium.net/))

## Tính năng

- ✅ REST API hoàn chỉnh với Spring Boot
- ✅ Xác thực người dùng với JWT
- ✅ Đăng ký và đăng nhập
- ✅ Tạo gói SCORM 2004 4th Edition
- ✅ Quản lý gói SCORM (CRUD)
- ✅ Tích hợp SCORM API đầy đủ
- ✅ Hỗ trợ tracking điểm số và tiến độ
- ✅ Tương thích với các LMS phổ biến
- ✅ CORS hỗ trợ cho frontend web

## Yêu cầu hệ thống

- Java 11 hoặc cao hơn
- Maven 3.6+
- Ít nhất 512MB RAM
- 100MB dung lượng đĩa trống

## Cài đặt và chạy

### Cách 1: Chạy nhanh (Khuyến nghị)

```bash
# 1. Clone hoặc tải về dự án
cd scorm-be

# 2. Build dự án
mvn clean install

# 3. Chạy ứng dụng
mvn spring-boot:run
```

API sẽ chạy tại: `http://localhost:8080/api`

### Cách 2: Build JAR và chạy

```bash
# Build JAR
mvn clean package

# Chạy JAR
java -jar target/scorm-package-generator-1.0.0.jar
```

### Cách 3: Chạy trong IDE

1. Mở project trong IntelliJ IDEA hoặc Eclipse
2. Tìm class `ScormGeneratorApplication`
3. Click "Run" hoặc nhấn Shift+F10

## Hướng dẫn sử dụng API

Xem chi tiết và các ví dụ request/response tại file [API_DOCUMENTATION.md](API_DOCUMENTATION.md).

### Các bước chính:

1.  **Đăng ký** tài khoản qua `POST /auth/register`.
2.  **Đăng nhập** qua `POST /auth/login` để nhận JWT token.
3.  Sử dụng token này trong header `Authorization: Bearer <token>` cho các request sau.
4.  **Tạo gói SCORM** qua `POST /scorm-packages`.
5.  **Quản lý** các gói đã tạo (lấy danh sách, xem chi tiết, xóa).

## Cấu trúc SCORM Package

Mỗi package được tạo sẽ chứa:

```
package.zip
├── imsmanifest.xml     # SCORM manifest file
├── index.html          # Main quiz interface
├── quiz.js            # Quiz logic và SCORM integration
├── scorm_api.js       # SCORM 2004 API wrapper
└── style.css          # Styling cho quiz
```

## Tương thích LMS

Đã test trên:

- Moodle 3.9+
- Canvas
- Blackboard Learn
- SCORM Cloud
- Adobe Captivate Prime

## Tính năng SCORM được hỗ trợ

- ✅ `cmi.completion_status` - Trạng thái hoàn thành
- ✅ `cmi.success_status` - Trạng thái đạt/không đạt
- ✅ `cmi.score.raw` - Điểm thô
- ✅ `cmi.score.scaled` - Điểm chuẩn hóa (0-1)
- ✅ `cmi.progress_measure` - Tiến độ học
- ✅ `cmi.session_time` - Thời gian học
- ✅ `cmi.interactions` - Chi tiết tương tác từng câu hỏi

## Troubleshooting

### ❌ Lỗi "Java không được tìm thấy"

```bash
# Kiểm tra Java đã cài đặt chưa
java -version

# Nếu chưa có, tải Java từ:
# https://adoptium.net/ (Eclipse Temurin - Khuyến nghị)
# hoặc https://www.oracle.com/java/technologies/downloads/
```

### ❌ Lỗi "Maven not found"

```bash
# Kiểm tra Maven đã cài đặt chưa
mvn -version

# Tải Maven từ: https://maven.apache.org/download.cgi
# Hoặc sử dụng package manager:
# Windows (Chocolatey): choco install maven
# macOS (Homebrew): brew install maven
# Linux (apt): sudo apt-get install maven
```

### ❌ Port 8080 đã được sử dụng

```bash
# Thay đổi port trong application.yml
# Hoặc chạy với port khác:
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

### ❌ Lỗi build Maven

```bash
# Xóa cache Maven và build lại
mvn clean install -U

# Hoặc xóa .m2 folder (Windows)
rmdir /s %USERPROFILE%\.m2\repository
mvn clean install

# Hoặc xóa .m2 folder (macOS/Linux)
rm -rf ~/.m2/repository
mvn clean install
```

### ❌ Token hết hạn

Token JWT có thời hạn 24 giờ. Nếu hết hạn, hãy đăng nhập lại để lấy token mới.

### ❌ SCORM package không hoạt động trên LMS

1. **Kiểm tra file imsmanifest.xml** có hợp lệ không
2. **Đảm bảo LMS hỗ trợ SCORM 2004** 4th Edition
3. **Kiểm tra console browser** (F12) để xem lỗi JavaScript
4. **Test trên SCORM Cloud** trước khi upload lên LMS chính
5. **Kiểm tra file size** - một số LMS giới hạn kích thước upload

## Phát triển thêm

### Thêm loại câu hỏi mới

1. Extend `Question` model
2. Update `HtmlContentGenerator`
3. Modify `quiz.js` để xử lý loại câu hỏi mới

### Tùy chỉnh giao diện

1. Chỉnh sửa `style.css` trong resources
2. Update `HtmlContentGenerator` để thay đổi HTML structure

### Thêm tính năng SCORM

1. Extend `ScormAPI` class trong `scorm_api.js`
2. Update `quiz.js` để sử dụng API mới

## Giấy phép

MIT License - Xem file LICENSE để biết chi tiết.

## Hỗ trợ

Nếu gặp vấn đề, vui lòng:

1. Kiểm tra phần Troubleshooting
2. Tạo issue trên GitHub
3. Cung cấp log file và thông tin hệ thống
