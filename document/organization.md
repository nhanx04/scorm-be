# Organization API

## Base

- **Base URL**: `http://localhost:8080/api`
- **Auth**: Tất cả API dưới đây **yêu cầu** header `Authorization: Bearer <token>`.

---

## 1) Create organization

- **URL**: `POST /organizations`
- **Auth**: Yes

### Request JSON

```json
{
  "orgName": "My Org",
  "description": "Demo organization",
  "maxAuthors": 10,
  "logoMediaId": 123
}
```

### Response JSON (200)

```json
{
  "orgId": 1,
  "orgName": "My Org",
  "description": "Demo organization",
  "maxAuthors": 10,
  "logoMediaId": 123,
  "ownerId": 1
}
```

### Notes

- Khi tạo org thành công, hệ thống tự tạo membership cho owner:
  - `org_role = "OWNER"`
  - `status = "ACTIVE"`

---

## 2) Invite user to organization (Owner)

- **URL**: `POST /organizations/{orgId}/invite`
- **Auth**: Yes
- **Permission**: Chỉ **OWNER** mới được mời.

### Request JSON

```json
{
  "email": "invited@example.com",
  "role": "MEMBER"
}
```

### Response (200)

- Body rỗng.

### Notes

- `role` nếu không truyền sẽ default là `"MEMBER"`.
- Nếu user đã có membership trong org => trả lỗi.

---

## 3) Respond to invitation (Invitee)

- **URL**: `POST /organizations/{orgId}/invite/respond`
- **Auth**: Yes

### Request JSON

```json
{
  "accept": true
}
```

### Response (200)

- Body rỗng.

### Behavior

- `accept=true`:
  - `status` chuyển từ `INVITED` -> `ACTIVE`
  - set `joined_at = now()`
- `accept=false`:
  - `status` chuyển từ `INVITED` -> `REJECTED`

---

## 4) List my organizations

- **URL**: `GET /organizations/me`
- **Auth**: Yes

### Response JSON (200)

```json
[
  {
    "orgId": 1,
    "orgName": "My Org",
    "description": "Demo organization",
    "maxAuthors": 10,
    "logoMediaId": 123,
    "ownerId": 1
  }
]
```

---

## 5) List members of an organization

- **URL**: `GET /organizations/{orgId}/members`
- **Auth**: Yes

### Response JSON (200)

```json
[
  {
    "userId": 1,
    "email": "owner@example.com",
    "fname": "Nguyen",
    "minit": "",
    "lname": "An",
    "role": "OWNER",
    "status": "ACTIVE",
    "invitedAt": null,
    "joinedAt": "2026-01-18T10:00:00+07:00"
  },
  {
    "userId": 2,
    "email": "invited@example.com",
    "fname": "Tran",
    "minit": "",
    "lname": "B",
    "role": "MEMBER",
    "status": "INVITED",
    "invitedAt": "2026-01-18T10:05:00+07:00",
    "joinedAt": null
  }
]
```

---

## 6) Revoke invitation (Owner)

- **URL**: `DELETE /organizations/{orgId}/invite/{userId}`
- **Auth**: Yes
- **Permission**: Chỉ **OWNER**.

### Response (200)

- Body rỗng.

### Notes

- Chỉ revoke được nếu membership đang `INVITED`.

---

## 7) Remove member (Owner)

- **URL**: `DELETE /organizations/{orgId}/members/{userId}`
- **Auth**: Yes
- **Permission**: Chỉ **OWNER**.

### Response (200)

- Body rỗng.

### Notes

- Chỉ remove được nếu membership đang `ACTIVE`.
- Không thể remove owner.
