# SCORM Package Generator

Ứng dụng Java để tạo gói SCORM 2004 từ câu hỏi và đáp án với giao diện đồ họa đơn giản.

**Yêu cầu:** Java 11+ đã cài đặt ([Tải Java](https://adoptium.net/))

## Tính năng

- ✅ Giao diện đồ họa thân thiện với Swing
- ✅ Tạo và chỉnh sửa câu hỏi trắc nghiệm
- ✅ Xuất gói SCORM 2004 4th Edition
- ✅ Tích hợp SCORM API đầy đủ
- ✅ Hỗ trợ tracking điểm số và tiến độ
- ✅ Responsive HTML quiz interface
- ✅ Tương thích với các LMS phổ biến

## Yêu cầu hệ thống

- Java 11 hoặc cao hơn
- Maven 3.6+
- Ít nhất 512MB RAM
- 100MB dung lượng đĩa trống

## Cài đặt và chạy

```bash
# 1. Chạy script tải dependencies
run-app.bat
```

## Hướng dẫn sử dụng

### 1. Tạo bài kiểm tra mới

1. Mở ứng dụng
2. Nhập **Tiêu đề** bài kiểm tra
3. Nhập **Mô tả** (tùy chọn)
4. Thiết lập **Điểm đạt** (%) và **Số lần thử tối đa**

### 2. Thêm câu hỏi

1. Click **"Thêm câu hỏi"** trong danh sách bên trái
2. Chọn câu hỏi vừa tạo
3. Nhập nội dung câu hỏi ở panel bên phải
4. Thêm các đáp án (tối thiểu 2 đáp án)
5. Chọn đáp án đúng bằng radio button
6. Thêm giải thích (tùy chọn)

### 3. Xuất SCORM Package

1. Click **"Export SCORM"** trên toolbar
2. Chọn vị trí lưu file
3. Đặt tên file (tự động thêm .zip)
4. Click **"Save"**

### 4. Upload lên LMS

1. Đăng nhập vào LMS của bạn
2. Tìm chức năng import/upload SCORM package
3. Upload file .zip vừa tạo
4. Cấu hình các thiết lập cần thiết
5. Publish và test

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

### ❌ Lỗi "Dependencies not found"

```bash
# Chạy lại script tải dependencies
run-simple.bat

# Hoặc xóa thư mục lib và chạy lại
rmdir /s lib
run-simple.bat
```

### ❌ Lỗi compile

```bash
# Kiểm tra encoding
chcp 65001

# Xóa target và compile lại
rmdir /s target
run-simple.bat

# Nếu vẫn lỗi, kiểm tra file compile_error.log
```

### ❌ Ứng dụng không khởi động

```bash
# Kiểm tra JAVA_HOME (Windows)
echo %JAVA_HOME%

# Chạy với thông tin debug
java -verbose:class -cp "target\classes;lib\*" com.scorm.generator.ScormGeneratorApp
```

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
