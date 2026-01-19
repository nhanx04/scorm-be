# User API

## Base

- **Base URL**: `http://localhost:8080/api`
- **Auth**: dùng header `Authorization: Bearer <token>` cho các API cần đăng nhập.

---

## 1) Register

- **URL**: `POST /auth/register`
- **Auth**: No

### Request JSON

```json
{
  "fname": "Nguyen",
  "minit": "",
  "lname": "An",
  "email": "test@example.com",
  "password": "123456"
}
```

### Response JSON (200)

```json
{
  "token": "<jwt>",
  "user": {
    "userId": 1,
    "fname": "Nguyen",
    "minit": "",
    "lname": "An",
    "email": "test@example.com",
    "avatarUrl": null
  }
}
```

### Notes

- **Email** phải unique.

---

## 2) Login

- **URL**: `POST /auth/login`
- **Auth**: No

### Request JSON

```json
{
  "email": "test@example.com",
  "password": "123456"
}
```

### Response JSON (200)

```json
{
  "token": "<jwt>",
  "user": {
    "userId": 1,
    "fname": "Nguyen",
    "minit": "",
    "lname": "An",
    "email": "test@example.com",
    "avatarUrl": null
  }
}
```

---

## 3) Authorization header

Ví dụ:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```
