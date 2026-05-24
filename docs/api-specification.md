# i-Route AI 기능 API 명세서

**Base URL**: `http://localhost:8080`  
**인증**: 현재 전체 허용 (`anyRequest().permitAll()`)  
**작성일**: 2026-05-25

---

## 목차

1. [성적 관리 `/api/grades`](#1-성적-관리-apigrades)
2. [학습 기록 `/api/activities`](#2-학습-기록-apiactivities)
3. [AI 상담 리포트 `/api/counseling`](#3-ai-상담-리포트-apicounseling)
4. [오답 관리 `/api/wrong-answer`](#4-오답-관리-apiwrong-answer)
5. [학습 계획 `/api/study-plan`](#5-학습-계획-apistudy-plan)
6. [복습 알림 `/api/review`](#6-복습-알림-apireview)
7. [AI 추천 `/api/recommendations`](#7-ai-추천-apirecommendations)
8. [분석 리포트 `/api/analysis`](#8-분석-리포트-apianalysis)

---

## 1. 성적 관리 (`/api/grades`)

### 1-1. 성적 입력

```
POST /api/grades
Content-Type: application/json
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `studentId` | String | ✅ | 학생 ID |
| `subject` | String | ✅ | 과목명 |
| `score` | int | ✅ | 점수 |
| `gradeLevel` | int | ✅ | 학년 |
| `examType` | String | ✅ | 시험 유형 (예: "중간", "기말") |
| `examDate` | String | ✅ | 시험 날짜 (`yyyy-MM-dd`) |

**Request Example**

```json
{
  "studentId": "S-0155",
  "subject": "수학",
  "score": 85,
  "gradeLevel": 2,
  "examType": "중간",
  "examDate": "2025-04-15"
}
```

**Response** `200 OK`

```json
{
  "id": 1,
  "subject": "수학",
  "score": 85,
  "gradeLevel": 2,
  "examType": "중간",
  "examDate": "2025-04-15"
}
```

---

### 1-2. 성적 목록 조회

```
GET /api/grades/{studentId}
```

**Path Variable**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `studentId` | String | 학생 ID |

**Response** `200 OK`

```json
[
  {
    "id": 1,
    "subject": "수학",
    "score": 85,
    "gradeLevel": 2,
    "examType": "중간",
    "examDate": "2025-04-15"
  }
]
```

---

### 1-3. 성적 추이 분석

```
GET /api/grades/{studentId}/analysis
```

**Path Variable**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `studentId` | String | 학생 ID |

**Response** `200 OK`

```json
{
  "studentId": "S-0155",
  "gradeHistory": [
    {
      "id": 1,
      "subject": "수학",
      "score": 80,
      "gradeLevel": 2,
      "examType": "중간",
      "examDate": "2025-03-15"
    },
    {
      "id": 2,
      "subject": "수학",
      "score": 85,
      "gradeLevel": 2,
      "examType": "기말",
      "examDate": "2025-04-15"
    }
  ],
  "subjectScoreChanges": {
    "국어": 5,
    "수학": -3
  },
  "summaryMessage": "전월 대비 국어 성적이 5점 상승했습니다!"
}
```

---

### 1-4. AI 맞춤 족보 탐색 (비동기)

```
POST /api/grades/analyze
Content-Type: application/json
```

> AI 서버 호출은 비동기로 실행되며, 클라이언트에는 즉시 "접수 완료" 응답을 반환합니다.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `score` | int | ✅ | 이번 시험 점수 |
| `allScores` | int[] | ✅ | 전체 학생 점수 배열 (평균/표준편차 계산용) |
| `weakConceptTag` | String | ✅ | 틀린 주요 개념 태그 |

**Request Example**

```json
{
  "score": 72,
  "allScores": [55, 60, 72, 80, 91, 88, 45],
  "weakConceptTag": "이차방정식의 근과 계수"
}
```

**Response** `200 OK`

```
"성적 분석 및 AI 맞춤 족보 탐색이 성공적으로 시작되었습니다."
```

---

## 2. 학습 기록 (`/api/activities`)

### 2-1. 학습 기록 입력

```
POST /api/activities
Content-Type: application/json
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `studentId` | String | ✅ | 학생 ID |
| `subject` | String | ✅ | 과목명 |
| `studyDate` | String | ✅ | 학습 날짜 (`yyyy-MM-dd`) |
| `studyDurationMinutes` | int | ✅ | 학습 시간 (분) |
| `understandingScore` | int | ✅ | 자기 이해도 평가 (1~5) |
| `concentrationScore` | int | ✅ | 집중도 평가 (1~5) |

> 강사 피드백은 학습 기록 생성 후 `PATCH /api/activities/{activityId}/feedback`으로 별도 입력합니다.

**Request Example**

```json
{
  "studentId": "S-0155",
  "subject": "수학",
  "studyDate": "2025-05-20",
  "studyDurationMinutes": 90,
  "understandingScore": 4,
  "concentrationScore": 3
}
```

**Response** `200 OK`

```json
{
  "id": 1,
  "subject": "수학",
  "studyDate": "2025-05-20",
  "studyDurationMinutes": 90,
  "understandingScore": 4,
  "concentrationScore": 3,
  "instructorFeedback": null
}
```

---

### 2-2. 학습 기록 조회

```
GET /api/activities/{studentId}
```

**Path Variable**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `studentId` | String | 학생 ID |

**Response** `200 OK` — `LearningActivityResponse[]` 배열

---

### 2-3. 강사 피드백 입력/수정

```
PATCH /api/activities/{activityId}/feedback
Content-Type: application/json
```

**Path Variable**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `activityId` | Long | 학습 기록 ID |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `instructorFeedback` | String | ✅ | 강사 피드백 내용 |

**Request Example**

```json
{
  "instructorFeedback": "삼각함수 개념 이해는 좋으나 응용 문제 연습이 더 필요합니다."
}
```

**Response** `200 OK` — 업데이트된 `LearningActivityResponse`

**Error**

| 상태코드 | 설명 |
|----------|------|
| `404 Not Found` | 해당 `activityId`의 학습 기록이 존재하지 않음 |

---

## 3. AI 상담 리포트 (`/api/counseling`)

> Python AI 서버(`http://localhost:8082`)와 연동되는 비동기 API입니다.  
> AI 서버가 실행 중이지 않으면 오류가 발생합니다.

### 3-1. 수학 메타인지 분석 리포트

```
POST /api/counseling/math?studentId={studentId}
```

### 3-2. 진로 탐색 리포트

```
POST /api/counseling/writing?studentId={studentId}
```

### 3-3. i-Route 프리미엄 통합 리포트

```
POST /api/counseling/premium?studentId={studentId}
```

**Query Parameter** (세 API 공통)

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `studentId` | String | 학생 ID |

**Response** `200 OK` (세 API 공통)

```json
{
  "studentId": "S-0155",
  "title": "수학 메타인지 분석 리포트",
  "careerAnalysis": "현재 수학 자기평가 점수 대비 실제 성적 격차가 높습니다...",
  "learningGuide": "개념 위주의 반복 학습을 권장합니다..."
}
```

---

## 4. 오답 관리 (`/api/wrong-answer`)

### 4-1. 오답 기록

```
POST /api/wrong-answer/record
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `studentId` | String | ✅ | 학생 ID |
| `subject` | String | ✅ | 과목명 |
| `questionId` | String | ✅ | 문제 ID |
| `conceptTag` | String | ✅ | 틀린 개념 태그 |

**Request Example**

```
POST /api/wrong-answer/record?studentId=S-0155&subject=수학&questionId=Q-001&conceptTag=이차방정식
```

**Response** `200 OK` — 저장된 `WrongAnswer` 엔티티

---

### 4-2. AI 파이프라인용 오답 데이터 조회

```
GET /api/wrong-answer/ai-pipeline?studentId={id}&subject={subject}
```

> Python AI 서버가 학생의 취약점 데이터를 가져갈 때 사용하는 내부 API입니다.

**Query Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `studentId` | String | 학생 ID |
| `subject` | String | 과목명 |

**Response** `200 OK` — `WrongAnswer[]` 배열

---

## 5. 학습 계획 (`/api/study-plan`)

### 5-1. 취약 개념 기반 복습 문제지 생성

```
POST /api/study-plan/review-paper?studentId={id}
```

**Query Parameter**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `studentId` | Long | 학생 ID |

**Response** `200 OK`

```json
{
  "paperId": 1,
  "questions": [
    "이차방정식 x² - 5x + 6 = 0의 근을 구하시오.",
    "삼각함수 sin30°의 값은?"
  ],
  "weakConcepts": ["이차방정식", "삼각함수"]
}
```

---

### 5-2. 목표 기반 학습 로드맵 생성

```
POST /api/study-plan/progress?studentId={id}
Content-Type: application/json
```

**Query Parameter**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `studentId` | Long | 학생 ID |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `targetKeyword` | String | ✅ | 목표 키워드 (예: "수능 1등급") |
| `targetDate` | String | ✅ | 목표 달성일 (`yyyy-MM-dd`) |
| `dailyStudyHours` | int | ✅ | 하루 학습 가능 시간 (시간 단위) |

**Request Example**

```json
{
  "targetKeyword": "수능 1등급",
  "targetDate": "2025-11-15",
  "dailyStudyHours": 4
}
```

**Response** `200 OK`

```json
{
  "studentId": 1,
  "targetKeyword": "수능 1등급",
  "targetDate": "2025-11-15",
  "weeklyMilestones": [
    "1주차: 수학 개념 정립 (이차방정식, 삼각함수)",
    "2주차: 수학 기출 풀이",
    "3주차: 국어 비문학 집중",
    "4주차: 모의고사 실전 연습"
  ],
  "overallStrategy": "취약 단원 집중 보완 후 전 과목 균형 학습 전략"
}
```

---

## 6. 복습 알림 (`/api/review`)

### 6-1. 오늘의 복습 항목 조회

```
GET /api/review/today?studentId={id}
```

> 에빙하우스 망각 곡선 기반 — 1일 / 3일 / 7일 / 14일 / 30일 후 복습 대상을 반환합니다.

**Query Parameter**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `studentId` | String | 학생 ID |

**Response** `200 OK`

```json
{
  "studentId": "S-0155",
  "hasReview": true,
  "reviews": [
    {
      "title": "이차방정식",
      "dayLabel": "7일 후 복습",
      "originalDate": "2025-05-18",
      "message": "7일 전에 학습한 내용입니다. 지금 복습하세요!"
    },
    {
      "title": "삼각함수",
      "dayLabel": "1일 후 복습",
      "originalDate": "2025-05-24",
      "message": "어제 학습한 내용입니다. 오늘 복습하세요!"
    }
  ]
}
```

---

## 7. AI 추천 (`/api/recommendations`)

### 7-1. 학습 자료 추천

```
GET /api/recommendations/materials?studentId={id}
```

**Query Parameter**: `studentId` (Long)

**Response** `200 OK`

```json
[
  {
    "materialId": 1,
    "title": "이차방정식 기초 개념 강의",
    "materialType": "VIDEO",
    "matchReason": "취약 개념 '이차방정식' 매칭"
  },
  {
    "materialId": 2,
    "title": "수능 수학 기출 문제집",
    "materialType": "BOOK",
    "matchReason": "현재 학습 수준 적합"
  }
]
```

---

### 7-2. 목표 기반 학습 로드맵 추천

```
GET /api/recommendations/roadmap?studentId={id}
```

**Query Parameter**: `studentId` (Long)

**Response** `200 OK` — `StudyRoadmapDto`

```json
{
  "studentId": 1,
  "targetKeyword": "수능 1등급",
  "targetDate": "2025-11-15",
  "weeklyMilestones": ["1주차: ...", "2주차: ..."],
  "overallStrategy": "..."
}
```

---

### 7-3. 에빙하우스 기준 당일 복습 대상

```
GET /api/recommendations/daily-review?studentId={id}
```

**Query Parameter**: `studentId` (Long)

**Response** `200 OK` — `WrongAnswerEntity[]` 배열 (오늘 복습이 필요한 오답 목록)

---

### 7-4. 학습 성향 기반 공부법 추천

```
GET /api/recommendations/study-method?studentId={id}&subjectId={id}
```

**Query Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `studentId` | Long | 학생 ID |
| `subjectId` | Long | 과목 ID |

**Response** `200 OK`

```json
{
  "studentId": 1,
  "subjectId": 2,
  "tendency": "VISUAL",
  "studyGuide": "시각적 학습자 유형입니다. 도식화 및 마인드맵 활용을 추천합니다.",
  "recommendedApproach": "개념을 그림/도표로 정리하는 마인드맵 학습법 추천"
}
```

> `tendency` 값: `VISUAL` / `AUDITORY` / `KINESTHETIC`

---

### 7-5. 유사 성적대 학생 콘텐츠 추천

```
GET /api/recommendations/peer-content?studentId={id}
```

**Query Parameter**: `studentId` (String)

**Response** `200 OK` — `MaterialRecommendationDto[]`

---

### 7-6. 선배 성공 학습 경로 추천

```
GET /api/recommendations/peer-path?studentId={id}&subject={subject}
```

**Query Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `studentId` | String | 학생 ID |
| `subject` | String | 과목명 |

**Response** `200 OK`

```json
{
  "subject": "수학",
  "similarStudentsCount": 15,
  "avgInitialScore": 62.5,
  "avgFinalScore": 88.3,
  "avgImprovement": 25.8,
  "studyStrategyPattern": "기출 중심 반복 학습 + 오답 노트",
  "successMessage": "비슷한 성적대에서 시작해 1등급을 달성한 15명의 경로를 분석했습니다.",
  "keyInsights": [
    "개념 정리 후 기출 풀이",
    "오답 노트 꾸준히 활용",
    "주 3회 이상 모의고사 풀기"
  ]
}
```

---

## 8. 분석 리포트 (`/api/analysis`)

### 8-1. 점수 분석 리포트 (레거시)

```
GET /api/analysis/report?studentId={id}&subject={subject}
```

**Query Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `studentId` | String | 학생 ID |
| `subject` | String | 과목명 |

**Response** `200 OK`

```json
{
  "subjectName": "수학",
  "myPercentile": 78.5,
  "averageScore": 72.3,
  "standardDeviation": 12.1,
  "scoreChangeFromPrevious": 5.0,
  "trendSummary": "상승",
  "weakPointSummary": "함수 단원 취약"
}
```

> `trendSummary` 값: `"상승"` / `"하락"` / `"유지"`

---

### 8-2. 특정 시험 종합 리포트

```
GET /api/analysis/score-report?studentId={id}&testId={id}
```

**Query Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `studentId` | Long | 학생 ID |
| `testId` | Long | 시험 ID |

**Response** `200 OK` — `AnalysisReportDto`

---

### 8-3. 집중 시간대 및 과목 밸런스 분석

```
GET /api/analysis/study-pattern?studentId={id}
```

**Query Parameter**: `studentId` (Long)

**Response** `200 OK`

```json
{
  "studentId": 1,
  "goldenTime": "오후 7시~9시",
  "subjectStudyMinutes": {
    "1": 120,
    "2": 90,
    "3": 45
  },
  "studyBalanceSummary": "수학 편중 학습 경향 — 국어/영어 학습 시간 확보 필요"
}
```

---

### 8-4. 강점 단원 분석

```
GET /api/analysis/strengths?studentId={id}
```

**Query Parameter**: `studentId` (Long)

**Response** `200 OK`

```json
{
  "studentId": 1,
  "strongSubjectIds": [2, 5],
  "strengthSummary": "영어·사회 과목 정답률 상위 — 해당 과목은 유지 전략 권장"
}
```

---

### 8-5. 다음 시험 예상 점수

```
GET /api/analysis/predict?studentId={id}&subjectId={id}
```

**Query Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `studentId` | Long | 학생 ID |
| `subjectId` | Long | 과목 ID |

**Response** `200 OK`

```json
{
  "expectedScoreMin": 80,
  "expectedScoreMax": 92,
  "achievementProbability": 0.74
}
```

> `achievementProbability`: 0.0~1.0 사이 달성 확률

---

### 8-6. 메타인지 역량 진단

```
GET /api/analysis/meta-cognition?studentId={id}&subject={subject}
```

**Query Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `studentId` | String | 학생 ID |
| `subject` | String | 과목명 |

**Response** `200 OK`

```json
{
  "studentId": "S-0155",
  "subject": "수학",
  "avgUnderstandingScore": 4.2,
  "avgActualScore": 68.5,
  "scaledUnderstandingScore": 84.0,
  "gapScore": 15.5,
  "gapLevel": "HIGH",
  "gapSummary": "자기평가가 실제 점수보다 15.5점 높습니다",
  "advice": "개념 이해도를 재점검하고 실전 문제 풀이 비중을 높이세요"
}
```

> `gapLevel` 값: `HIGH` / `MEDIUM` / `LOW`

---

## 주의 사항

| 항목 | 내용 |
|------|------|
| `studentId` 타입 혼재 | API마다 `String` / `Long` 타입이 다릅니다. 프론트엔드 연동 시 주의 필요 |
| AI 서버 의존 | `/api/counseling/*` 3개 API는 Python AI 서버(`http://localhost:8082`) 실행 필요 |
| 비동기 처리 | `/api/grades/analyze`는 즉시 응답 후 백그라운드에서 AI 처리 실행 |
| 에빙하우스 복습 | `/api/review/today`, `/api/recommendations/daily-review` — 1·3·7·14·30일 주기 |
