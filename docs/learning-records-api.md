# 오답·OCR 채점·집중도 기록 API

기준 브랜치: `origin/develop`. 모든 아래 API는 기존 SecurityConfig의 JWT 인증을 사용한다
(`Authorization: Bearer <accessToken>`). AI 내부용 `/api/wrong-answer/ai-pipeline`은 기존 동작을 유지한다.
학생 ID는 `Student.studentId`의 숫자 값이며 `gradeStudentId` 문자열이 아니다.

## 학생별 전체 오답

`GET /api/wrong-answer/{studentId}`

- `?subject=국어` 선택 필터. 생략하면 전 과목을 반환한다.
- 응답은 기존 `WrongAnswer` 형식의 배열: `id`, `studentId`, `subject`, `questionId`,
  `conceptTag`, `errorType`, `failCount`, `createdAt`.
- `createdAt DESC, id DESC` 순서. 결과가 없으면 `[]`.
- 반복해서 틀린 문항은 기존 기록 방식대로 `failCount`로 누적된다.

## OCR 결과 저장

`POST /api/ocr-results`

```json
{
  "studentId": 14,
  "subject": "국어",
  "submissionId": "f228b055-30e8-4de8-ae87-11e47a871e02",
  "totalQuestions": 2,
  "correctCount": 1,
  "questions": [
    {
      "questionId": "worksheet-1-q1",
      "expectedAnswer": "돼지",
      "recognizedText": "되지",
      "similarity": 0.5,
      "correct": false,
      "conceptTag": "받침",
      "errorType": "MEMORIZATION_GAP"
    },
    {
      "questionId": "worksheet-1-q2",
      "expectedAnswer": "수박",
      "recognizedText": "수박",
      "similarity": 1.0,
      "correct": true,
      "conceptTag": "명사"
    }
  ]
}
```

- 성공 `200`: 요청 필드와 서버 생성 `id`, UTC `gradedAt`을 반환한다. 문항 순서를 유지한다.
- 총 문항 수, 정답 수, 모든 문항의 인식 문자열을 보존한다. 전부 정답이어도 저장한다.
- `totalQuestions`는 문항 배열 길이, `correctCount`는 `correct=true` 문항 수와 같아야 한다.
- 문항은 1~1000개, `questionId`는 제출 내에서 중복 불가. 이미지마다 1부터 다시 시작하는
  번호 대신 워크시트까지 포함한 안정적인 문항 ID를 쓰면 기존 오답 누적과 혼동되지 않는다.
- 인식 실패는 `recognizedText: ""`로 저장할 수 있다. 누락/null은 허용하지 않는다.
- `similarity`는 0~1. `errorType`은 기존 enum 값 또는 생략/null.
- 문항별 오답은 기존 `WrongAnswerService`를 통해 같은 DB 트랜잭션에 기록한다.
  호출자가 `/api/wrong-answer/record`를 다시 호출하면 중복 누적되므로 호출하지 않는다.
- 재전송할 때는 같은 `submissionId`와 같은 요청을 사용한다. 이미 저장되었다면 기존 결과를
  반환하고 오답 횟수를 늘리지 않는다. 같은 학생·제출 ID로 다른 내용이 오면 `409`.
  DB의 학생·제출 ID 유일 제약으로 동시 요청의 중복 행도 방지한다.
- 형식·개수·범위 검증 실패는 `400`. 이 API는 인식/채점 모델을 실행하지 않고 AI가 판정한 결과를 저장한다.

## OCR 결과 조회

- `GET /api/ocr-results?studentId=14`: 학생의 전체 채점 결과, `gradedAt DESC, id DESC`.
- `GET /api/ocr-results?studentId=14&subject=국어`: 과목별 조회.
- `GET /api/ocr-results/{id}?studentId=14`: 한 채점 결과 상세. 학생 ID 불일치 또는 없는 결과는 `404`.
- 목록과 상세 모두 문항별 인식 문자열·정답·유사도·정오답을 포함한다. 빈 목록은 `[]`.

## 집중도 상세값

기존 `POST /api/activities`, `GET /api/activities/{studentId}`에 다음 필드를 추가한다.

| 필드 | 형식 | 의미 |
|---|---|---|
| `focusedRatio` | number 또는 null | 집중 표본 비율, 0~1 |
| `drowsyRatio` | number 또는 null | 졸음 표본 비율, 0~1 |
| `measurementSource` | `CAMERA` / `MANUAL` | 집중도 측정 출처 |

```json
{
  "studentId": 14,
  "subject": "수학",
  "studyDate": "2026-11-04",
  "studyStartTime": "10:00:00",
  "studyDurationMinutes": 30,
  "understandingScore": 4,
  "concentrationScore": 3,
  "focusedRatio": 0.6,
  "drowsyRatio": 0.2,
  "measurementSource": "CAMERA"
}
```

- 별점 `understandingScore`, `concentrationScore`는 기존과 같은 1~5점. 비율을 대신 넣지 않는다.
- `CAMERA`일 때 두 비율은 필수이며 합은 1 이하여야 한다. 나머지는 집중하지 않음 상태다.
- Pi에서는 얼굴 미검출 표본을 비율의 분모에서 제외한다. 검출 표본이 없으면 업로드하지 않는다.
- 출처를 생략하거나 null로 보내면 `MANUAL`. 이전 앱 요청은 상세 비율 없이도 저장 가능하다.
- 기존 DB 행의 출처가 null인 경우 응답에서 `MANUAL`로 취급한다. 과거 카메라 기록은 구분할
  근거가 없으므로 소급 복원하지 않는다. 비율이 없는 경우 JSON 설정에 따라 필드가 생략될 수 있다.

## 스키마와 검증

기존 설정 `spring.jpa.hibernate.ddl-auto=update`를 사용하면 시작 시 `ocr_grading_result`,
`ocr_question_result` 테이블과 `learning_activity`의 nullable 컬럼 `focused_ratio`,
`drowsy_ratio`, `measurement_source`를 생성한다. 운영 DB에는 이 작업에서 접속하거나 변경하지 않았다.
DDL 자동 갱신을 끈 환경에서는 같은 엔티티 스키마를 별도 마이그레이션으로 반영해야 한다.

```bash
./gradlew learningRecordsTest bootJar
```

`learningRecordsTest`는 H2 실제 저장·조회와 MockMvc 요청 검증을 실행한다. 기존 `test` 소스의
문자열 학생 ID/삭제된 게시판 타입에 따른 컴파일 오류와 독립 실행하도록 분리했다.
