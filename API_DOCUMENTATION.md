# SCORM Package Generator API

REST API Backend cho việc tạo gói SCORM 2004 từ các câu hỏi và đáp án.

## Cài đặt và Chạy

### Yêu cầu

- Java 11 hoặc cao hơn
- Maven 3.6+

### Chạy ứng dụng

```bash
mvn clean install
mvn spring-boot:run
```

API sẽ chạy tại: `http://localhost:8080/api`

## API Endpoints

### 1. Đăng ký (Register)

**Endpoint:** `POST /auth/register`

**Request Body:**

```json
{
  "email": "user@example.com",
  "password": "password123",
  "fullName": "Tên người dùng"
}
```

**Response (201 Created):**

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "userId": 1,
  "email": "user@example.com",
  "fullName": "Tên người dùng"
}
```

---

### 2. Đăng nhập (Login)

**Endpoint:** `POST /auth/login`

**Request Body:**

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response (200 OK):**

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "userId": 1,
  "email": "user@example.com",
  "fullName": "Tên người dùng"
}
```

---

### 3. Tạo gói SCORM (Create SCORM Package)

**Endpoint:** `POST /scorm-packages`

**Headers:**

```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body (Ví dụ đầy đủ):**

```json
{
  "title": "Bài kiểm tra Tổng hợp",
  "description": "Một bài kiểm tra với nhiều loại câu hỏi và tùy chỉnh.",
  "passingScore": 80,
  "maxAttempts": 2,
  "welcomeVideoUrl": "https://www.youtube.com/embed/your_video_id", // QUAN TRỌNG: Chỉ dán URL từ thuộc tính 'src' của mã nhúng, không phải toàn bộ thẻ <iframe>
  "themeJson": "{\"primaryColor\": \"#4CAF50\", \"backgroundColor\": \"#f0f9f0\", \"fontFamily\": \"'Roboto', sans-serif\"}",
  "reviewMode": "REVIEW_WITH_ANSWERS", // Chế độ xem lại: NO_REVIEW, REVIEW_WITHOUT_ANSWERS, REVIEW_WITH_ANSWERS

  "questions": [
    {
      "text": "Đâu là hình ảnh của một con mèo?",
      "questionType": "MULTIPLE_CHOICE",
      "imageUrl": "https://your-s3-bucket.s3.region.amazonaws.com/media/cat_image.jpg",
      "questionOrder": 0,
      "answers": [
        {
          "text": "Đây là con chó",
          "correct": false,
          "answerOrder": 0,
          "imageUrl": "https://your-s3-bucket.s3.region.amazonaws.com/media/dog_image.jpg"
        },
        {
          "text": "Đây là con mèo",
          "correct": true,
          "answerOrder": 1,
          "imageUrl": "https://your-s3-bucket.s3.region.amazonaws.com/media/cat_image.jpg"
        },
        {
          "text": "Đây là con vịt",
          "correct": false,
          "answerOrder": 2,
          "imageUrl": "https://your-s3-bucket.s3.region.amazonaws.com/media/duck_image.jpg"
        }
      ]
    },
    {
      "text": "Trái đất quay quanh mặt trời.",
      "questionType": "TRUE_FALSE",
      "imageUrl": null,
      "questionOrder": 1,
      "answers": [
        {
          "text": "Đúng",
          "correct": true,
          "answerOrder": 0,
          "matchValue": null
        }
      ]
    },
    {
      "text": "Nối các quốc gia với thủ đô của chúng.",
      "questionType": "MATCHING",
      "imageUrl": null,
      "questionOrder": 2,
      "answers": [
        {
          "text": "Việt Nam",
          "matchValue": "Hà Nội",
          "correct": true,
          "answerOrder": 0
        },
        {
          "text": "Nhật Bản",
          "matchValue": "Tokyo",
          "correct": true,
          "answerOrder": 1
        },
        {
          "text": "Pháp",
          "matchValue": "Paris",
          "correct": true,
          "answerOrder": 2
        }
      ]
    },
    {
      "text": "Thủ đô của Việt Nam là gì?",
      "questionType": "SHORT_ANSWER",
      "imageUrl": null,
      "questionOrder": 3,
      "answers": [
        {
          "text": "Hà Nội",
          "correct": true,
          "answerOrder": 0,
          "matchValue": null
        },
        {
          "text": "hanoi",
          "correct": true,
          "answerOrder": 1,
          "matchValue": null
        }
      ]
    }
  ]
}
```

**Response (201 Created):**

```json
{
  "id": 1,
  "title": "Bài kiểm tra Toán học",
  "description": "Bài kiểm tra về phép cộng và phép trừ",
  "passingScore": 70,
  "maxAttempts": 3,
  "packageUrl": "https://YOUR_S3_BUCKET_NAME.s3.YOUR_AWS_REGION.amazonaws.com/scorm/Bai_kiem_tra_Toan_hoc_20231215_143022.zip",
  "createdAt": "2023-12-15T14:30:22",
  "updatedAt": "2023-12-15T14:30:22",
  "questions": [...]
}
```

---

### 4. Lấy danh sách gói SCORM của người dùng

**Endpoint:** `GET /scorm-packages`

**Headers:**

```
Authorization: Bearer <token>
```

**Response (200 OK):**

```json
[
  {
    "id": 1,
    "title": "Bài kiểm tra Toán học",
    "description": "Bài kiểm tra về phép cộng và phép trừ",
    "passingScore": 70,
    "maxAttempts": 3,
    "packageUrl": "https://YOUR_S3_BUCKET_NAME.s3.YOUR_AWS_REGION.amazonaws.com/scorm/Bai_kiem_tra_Toan_hoc_20231215_143022.zip",
    "createdAt": "2023-12-15T14:30:22",
    "updatedAt": "2023-12-15T14:30:22",
    "questions": [...]
  }
]
```

---

### 5. Lấy chi tiết gói SCORM

**Endpoint:** `GET /scorm-packages/{packageId}`

**Headers:**

```
Authorization: Bearer <token>
```

**Response (200 OK):**

```json
{
  "id": 1,
  "title": "Bài kiểm tra Toán học",
  "description": "Bài kiểm tra về phép cộng và phép trừ",
  "passingScore": 70,
  "maxAttempts": 3,
  "packageUrl": "https://YOUR_S3_BUCKET_NAME.s3.YOUR_AWS_REGION.amazonaws.com/scorm/Bai_kiem_tra_Toan_hoc_20231215_143022.zip",
  "createdAt": "2023-12-15T14:30:22",
  "updatedAt": "2023-12-15T14:30:22",
  "questions": [...]
}
```

---

### 6. Tải lên Media (Upload Media)

**Endpoint:** `POST /media/upload`

**Headers:**

```
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

**Request Body:**

- `file`: Chọn một file ảnh hoặc video từ máy của bạn.

**Response (200 OK):**

- Một chuỗi (string) chứa URL công khai của file vừa được tải lên trên S3.

**Ví dụ cURL:**

```bash
curl -X POST http://localhost:8080/api/media/upload \
  -H "Authorization: Bearer <token>" \
  -F "file=@/path/to/your/image.jpg"
```

---

### 6. Xóa gói SCORM

**Endpoint:** `DELETE /scorm-packages/{packageId}`

**Headers:**

```
Authorization: Bearer <token>
```

**Response (204 No Content)**

---

## Lỗi phổ biến

| Status Code | Mô tả                                          |
| ----------- | ---------------------------------------------- |
| 400         | Bad Request - Dữ liệu không hợp lệ             |
| 401         | Unauthorized - Token không hợp lệ hoặc hết hạn |
| 404         | Not Found - Tài nguyên không tìm thấy          |
| 500         | Internal Server Error - Lỗi máy chủ            |

## Cấu hình

### JWT Secret

Thay đổi `jwt.secret` trong `application.yml` để bảo mật:

```yaml
jwt:
  secret: your-very-long-secret-key-here-change-this-in-production
  expiration: 86400000 # 24 hours
```

### Database

Hiện tại sử dụng H2 (in-memory). Để sử dụng MySQL:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/scormdb
    username: root
    password: your-password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    database-platform: org.hibernate.dialect.MySQL8Dialect
    hibernate:
      ddl-auto: update
```

## Ví dụ sử dụng cURL

### Đăng ký

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "fullName": "Tên người dùng"
  }'
```

### Đăng nhập

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

### Tạo gói SCORM

```bash
curl -X POST http://localhost:8080/api/scorm-packages \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d @scorm-request.json
```

## Lưu ý

- Token JWT có thời hạn 24 giờ
- Gói SCORM được lưu tại: `~/scorm-packages/`
- Mỗi người dùng chỉ có thể truy cập gói SCORM của chính họ
