# 출결 API 명세서

**Base URL:** `http://localhost:8080`

### 권한 정책

| 구분 | 설명 |
|---|---|
| 인증 불필요 | 토큰 없이 호출 가능 |
| 인증 필요 | JWT 토큰 필요 (`Authorization: Bearer {token}`) |
| ACADEMY / ADMIN | 학원 강사 또는 관리자 계정만 호출 가능 |

---

## 1. NFC 태그 — 승/하차 처리

라즈베리파이(PN532)에서 카드 태그 시 호출. 마지막 기록 기준으로 승/하차 자동 판단.

- **권한:** 인증 불필요

```
POST /api/gps/attendance
```

**Request Body**

```json
{
  "busId": 1,
  "nfcCardId": "AABBCCDD"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| busId | Long | ✅ | 버스 ID |
| nfcCardId | String | ✅ | NFC 카드 UID |

**Response `200 OK`**

```json
{
  "attendanceId": 5,
  "studentId": 1,
  "studentName": "김철수",
  "busId": 1,
  "eventType": "BOARD",
  "timestamp": "2026-05-31T16:23:35.669"
}
```

| eventType | 설명 |
|---|---|
| BOARD | 승차 |
| EXIT | 하차 |

**오류**

| 상태코드 | 설명 |
|---|---|
| 404 | 등록되지 않은 NFC 카드 |

---

## 2. 자녀 목록 조회

학부모가 로그인 후 자신의 userId로 자녀 정보 조회. 출결/성적 조회에 필요한 ID 포함.

- **권한:** 인증 불필요

```
GET /api/gps/parents/{parentId}/children
```

| 파라미터 | 타입 | 설명 |
|---|---|---|
| parentId | Long | 학부모 userId |

**Response `200 OK`**

```json
[
  {
    "gpsStudentId": 1,
    "gradeStudentId": "S-0155",
    "name": "김철수",
    "busId": 1
  }
]
```

| 필드 | 설명 |
|---|---|
| gpsStudentId | 출결 API 호출 시 사용하는 studentId |
| gradeStudentId | 성적 API 호출 시 사용하는 studentId |

> `gradeStudentId`는 관리자가 등록하기 전까지 null

---

## 3. 자녀 출결 이력 조회 (parentId 기반)

학부모 앱에서 자신의 userId로 자녀 출결 조회. 자녀 여러 명일 경우 전원 포함.

- **권한:** 인증 불필요

```
GET /api/gps/attendance/parents/{parentId}?date=2026-05-31
```

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| parentId | Long | ✅ | 학부모 userId |
| date | String (yyyy-MM-dd) | ❌ | 조회 날짜, 기본값: 오늘 |

**Response `200 OK`**

```json
[
  {
    "attendanceId": 6,
    "studentId": 1,
    "studentName": "김철수",
    "busId": 1,
    "eventType": "EXIT",
    "timestamp": "2026-05-31T16:23:43.655"
  },
  {
    "attendanceId": 5,
    "studentId": 1,
    "studentName": "김철수",
    "busId": 1,
    "eventType": "BOARD",
    "timestamp": "2026-05-31T16:23:35.669"
  }
]
```

> 최신순 정렬

---

## 4. 자녀 출결 이력 조회 (studentId 기반)

- **권한:** 인증 불필요

```
GET /api/gps/attendance/students/{studentId}?date=2026-05-31
```

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| studentId | Long | ✅ | 학생 ID (gpsStudentId) |
| date | String (yyyy-MM-dd) | ❌ | 조회 날짜, 기본값: 오늘 |

**Response:** 3번과 동일

---

## 5. 버스 전체 출결 조회

오늘 날짜 기준 해당 버스에 탑승한 모든 학생의 출결 조회. 시간순 정렬.

- **권한:** ACADEMY / ADMIN

```
GET /api/gps/attendance/buses/{busId}
```

**Header**
```
Authorization: Bearer {accessToken}
```

| 파라미터 | 타입 | 설명 |
|---|---|---|
| busId | Long | 버스 ID |

**Response `200 OK`**

```json
[
  {
    "attendanceId": 5,
    "studentId": 1,
    "studentName": "김철수",
    "busId": 1,
    "eventType": "BOARD",
    "timestamp": "2026-05-31T16:23:35.669"
  }
]
```

**오류**

| 상태코드 | 설명 |
|---|---|
| 403 | 권한 없음 (PARENT, DRIVER 계정) |

---

## 6. NFC 카드 등록

학생에게 NFC 카드 UID 등록. 학원 강사 또는 관리자가 수행. 이미 다른 학생에게 등록된 카드는 거부.

- **권한:** ACADEMY / ADMIN

```
PATCH /api/gps/students/{studentId}/nfc
```

**Header**
```
Authorization: Bearer {accessToken}
```

| 파라미터 | 타입 | 설명 |
|---|---|---|
| studentId | Long | 학생 ID |

**Request Body**

```json
{
  "nfcCardId": "AABBCCDD"
}
```

**Response `200 OK`** (body 없음)

**오류**

| 상태코드 | 설명 |
|---|---|
| 403 | 권한 없음 |
| 404 | 학생을 찾을 수 없음 |
| 409 | 이미 다른 학생에게 등록된 카드 |

---

## 7. 성적 시스템 ID 등록

학생에게 성적 조회용 gradeStudentId 등록. 등록 후 자녀 목록 조회 시 gradeStudentId가 반환되어 성적 API 연동 가능.

- **권한:** ACADEMY / ADMIN

```
PATCH /api/gps/students/{studentId}/grade-id
```

**Header**
```
Authorization: Bearer {accessToken}
```

| 파라미터 | 타입 | 설명 |
|---|---|---|
| studentId | Long | 학생 ID |

**Request Body**

```json
{
  "gradeStudentId": "S-0155"
}
```

**Response `200 OK`** (body 없음)

**오류**

| 상태코드 | 설명 |
|---|---|
| 403 | 권한 없음 |
| 404 | 학생을 찾을 수 없음 |
