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

`learningRecordsTest`는 H2 실제 저장·조회와 MockMvc 요청 검증을 실행한다.
기존 테스트의 학생 ID·게시판·인증 계약을 현재 구현에 맞게 수정했으며,
표준 `test`와 `check`도 `learningRecordsTest`를 함께 실행한다. 전체 검증은 `./gradlew clean check bootJar`로 실행한다.
# 2026-11-05 경계 조건 보완

- 오답·OCR·학습 활동 HTTP API는 본인 자녀 또는 소속 학원의 학생만 조회·저장할 수 있다. 운영자는 전체 접근할 수 있고 기사는 접근할 수 없다. 소속 외 학생은 403, 미로그인은 401이다.
- 강사 피드백 PATCH는 소속 학원/운영자만 가능하다. 활동 POST의 `instructorFeedback`은 저장하지 않는다.
- 자기평가 POST에 선택적인 `submissionId`(공백 제외, 최대 64자)를 전달하면 같은 학생/키/내용 재전송은 기존 기록을 반환한다. 같은 키의 내용 변경은 409이다. 직접 입력에서 키가 없으면 같은 학생/과목/날짜/시각/학습 시간/점수/측정값의 기존 기록을 재사용하며, 새 클라이언트가 키를 보내면 기존 무키 기록에 연결한다. 카메라 기록은 명시적 키로만 중복을 방지한다. 응답에도 `submissionId`가 포함된다.
- OCR/자기평가 저장은 학생 행의 DB 잠금으로 동시 요청도 직렬화한다. 서로 다른 과목의 같은 문항 ID는 별도 오답으로 누적한다.
- 학습 시간은 1~1440분이다. 잘못된 날짜·시각·매개변수는 400이다. 활동은 날짜/시각/ID 내림차순으로 정렬한다.
- 학습일은 운영 DB가 지원하는 1000-01-01~9999-12-31 범위로 제한하며, 범위 밖 연도는 저장 전에 400으로 거부한다. 윤년·23:59·최소/최대 학습 시간과 학부모/학원의 동시 저장을 실제 JWT·H2 테스트로 검증한다.
- 학생 ID·점수·문항 수·학습 시간의 소수 값은 절삭하지 않고 400으로 거부한다. `24:00:00` 같은 시각도 거부한다.
- 이메일 로그인도 갱신 토큰을 저장하며 무효/만료 토큰은 401이다. 새 갱신 토큰을 일반 API 접근 토큰으로 사용할 수 없다. 프론트는 동시 401의 갱신 요청을 공유하며 일시적인 통신 장애로 로그인 정보를 삭제하지 않는다.
- 프론트는 같은 자기평가 내용에서 동일한 SHA-256 저장 키를 만든다. 응답 유실 후 재시도·새로고침·여러 탭에서 같은 내용을 저장해도 중복 반영되지 않는다. 대량 오답/채점/문항은 더 보기로 나누어 렌더링한다.

검증은 전체 `test`와 `learningRecordsTest`의 권한/실제 JWT/DB 동시성/입력 경계 테스트 및 프론트 단위·브라우저 회귀 테스트로 수행한다. 라즈베리파이 실기기 연동은 검증 범위에서 제외한다.

## 2026-11-11 AI HAT 자세 기록

`POST /api/posture-records`로 자세 분석을 저장하고,
`GET /api/posture-records?studentId=2&subject=수학&limit=50`으로 조회한다.
JWT가 필요하며 본인 자녀·소속 학원 학생·운영자만 접근할 수 있다.
`subject`는 선택이고, `limit`은 기본 50, 최대 100이다. 측정 시작 시각/ID 최신순으로 반환한다.

요청 예:

```json
{
  "studentId": 2, "submissionId": "pose-example-1", "subject": "수학",
  "startedAt": "2026-11-10T10:00:00+09:00", "durationSeconds": 20,
  "inputSource": "VIDEO", "modelName": "yolov8s_pose_h8", "totalSamples": 10,
  "uprightSamples": 4, "slouchedSamples": 2, "headDownSamples": 1, "tiltedSamples": 1,
  "unknownSamples": 1, "noPersonSamples": 1, "multiplePeopleSamples": 0
}
```

- 입력 출처는 `CAMERA`, `IMAGE`, `VIDEO`이며 응답의 측정 장치는 `AI_HAT`이다.
- 모든 표본 수는 정수이며 자세별 7개 표본 수의 합이 `totalSamples`와 같아야 한다.
  전체 표본 수와 분석 시간(초)은 1~86400이다. 측정 시각은 시간대가 포함된 ISO 형식이며,
  저장·비교·응답은 DB 정밀도에 맞춰 UTC 초 단위로 통일한다(소수점 이하 절삭).
- 응답은 요청 정보와 `id`, `createdAt`, `measurementSource`, `classifiedSamples`,
  `uprightRatio`, `slouchedRatio`, `headDownRatio`, `tiltedRatio`를 포함한다.
  비율의 분모는 네 가지 자세로 판정된 표본 수이며, 판정 표본이 없으면 비율은 명시적 `null`이다.
- 학생/제출 ID가 같은 동일 내용 재전송은 기존 기록을 반환한다. 내용 변경은 409이며,
  동시 재전송도 학생 행 잠금과 DB 유일키로 한 건만 저장한다.
- 원본 영상·사진과 얼굴 집중도 점수를 저장하지 않는다. 자세 결과는 별도 `posture_record`에 저장한다.
- 이후 추가한 Hailo 실기기 검증은 공식 예제 영상/테스트 입력으로 수행한다. 실제 얼굴 측정과 PN532 연결은 별도이다.
- `subject`, `submissionId`, `modelName`은 JSON 문자열만 허용한다. 숫자·불리언을 문자열로 변환하지 않는다.
  공백·제로 폭 문자·제어 문자만 있는 과목과 잘못된 서로게이트 문자는 저장/조회 필터에서 400으로 거부한다.
  권한은 기존 제출 재전송에도 다시 확인하므로 소속이나 보호자가 변경되면 이전 계정의 접근은 거부된다.
