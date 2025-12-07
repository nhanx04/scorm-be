# Quick Start Guide - SCORM Package Generator API

## 1️⃣ Cài đặt và chạy

### Bước 1: Cài đặt Java

Đảm bảo Java 11+ đã được cài đặt:

```bash
java -version
```

### Bước 2: Cài đặt Maven

```bash
mvn -version
```

### Bước 3: Build và chạy

```bash
cd scorm-be
mvn clean install
mvn spring-boot:run
```

API sẽ chạy tại: **http://localhost:8080/api**

---

## 2️⃣ Sử dụng API

### Đăng ký người dùng

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "fullName": "Test User"
  }'
```

**Response:**

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "userId": 1,
  "email": "test@example.com",
  "fullName": "Test User"
}
```

### Đăng nhập

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

**Lưu token từ response để sử dụng cho các request tiếp theo!**

### Tạo gói SCORM

```bash
curl -X POST http://localhost:8080/api/scorm-packages \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Bài kiểm tra Toán",
    "description": "Bài kiểm tra cơ bản",
    "passingScore": 70,
    "maxAttempts": 3,
    "questions": [
      {
        "text": "2 + 2 = ?",
        "questionOrder": 0,
        "answers": [
          {"text": "3", "correct": false, "answerOrder": 0},
          {"text": "4", "correct": true, "answerOrder": 1},
          {"text": "5", "correct": false, "answerOrder": 2}
        ]
      }
    ]
  }'
```

### Lấy danh sách gói SCORM

```bash
curl -X GET http://localhost:8080/api/scorm-packages \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### Lấy chi tiết gói SCORM

```bash
curl -X GET http://localhost:8080/api/scorm-packages/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### Xóa gói SCORM

```bash
curl -X DELETE http://localhost:8080/api/scorm-packages/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## 3️⃣ Sử dụng Postman

1. Import file `postman_collection.json` vào Postman
2. Chạy request "Register" để tạo tài khoản
3. Copy token từ response
4. Paste token vào variable `{{token}}` trong Postman
5. Chạy các request khác

---

## 4️⃣ Cấu trúc dữ liệu

### CreateScormPackageRequest

```json
{
  "title": "Tiêu đề bài kiểm tra",
  "description": "Mô tả bài kiểm tra",
  "passingScore": 70,
  "maxAttempts": 3,
  "questions": [
    {
      "text": "Nội dung câu hỏi",
      "questionOrder": 0,
      "answers": [
        {
          "text": "Đáp án 1",
          "correct": false,
          "answerOrder": 0
        },
        {
          "text": "Đáp án 2",
          "correct": true,
          "answerOrder": 1
        }
      ]
    }
  ]
}
```

---

## 5️⃣ Các lỗi thường gặp

| Lỗi                          | Giải pháp                             |
| ---------------------------- | ------------------------------------- |
| Port 8080 đã được sử dụng    | Thay đổi port trong `application.yml` |
| Token không hợp lệ           | Đăng nhập lại để lấy token mới        |
| Email đã được đăng ký        | Sử dụng email khác                    |
| Câu hỏi không có đáp án đúng | Đánh dấu ít nhất 1 đáp án là đúng     |

---

## 6️⃣ Tài liệu chi tiết

- **API Documentation**: Xem [API_DOCUMENTATION.md](API_DOCUMENTATION.md)
- **README**: Xem [README.md](README.md)

---

## 7️⃣ Thông tin hữu ích

- **API Base URL**: `http://localhost:8080/api`
- **JWT Expiration**: 24 giờ
- **Database**: PostgreSQL (cần được cài đặt và cấu hình)
- **SCORM Packages**: Lưu trữ trên Amazon S3.

---

\*\*Chúc bạn sử dụng thành công! [object Object]
