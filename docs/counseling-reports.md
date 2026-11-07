# 저장된 AI 리포트 조회

`GET /api/counseling/reports?studentId=2`

`Authorization: Bearer <accessToken>`이 필요하다. 학부모는 본인 자녀, 학원은 현재 소속 학생만 조회할 수 있다. 운영자는 기존 학습 기록 정책에 따라 조회할 수 있고 기사 계정은 조회할 수 없다. 학생 소속은 서버 DB로 확인한다.

```json
[
  {
    "id": 123,
    "title": "수학 메타인지 분석 리포트",
    "subject": "수학",
    "careerAnalysis": "분석 본문",
    "learningGuide": "학습 안내",
    "createdAt": "2026-11-07T14:00:00"
  }
]
```

- `createdAt` 내림차순, 시간이 같으면 `id` 내림차순이다. 과거 날짜가 없는 행은 마지막에 나온다.
- 조회 가능한 학생에게 리포트가 없으면 `200 []`이다. AI 서버를 호출하거나 새 리포트를 생성하지 않는다.
- 미로그인·무효 토큰은 401, 타 가정/타 학원·학생 없음은 같은 403이다. 누락·문자·소수·0·음수 studentId는 400이다. 응답은 `Cache-Control: no-store`다.
- `subject`는 수학·국어·영어·과학·사회·통합이다. 통합은 프리미엄 리포트다. 새 생성/복습 추천 저장 시 과목을 기록한다.
- 기존 행은 기존 생성기의 알려진 제목으로만 과목을 보완한다. 알 수 없는 과목과 과거부터 없는 제목·본문·날짜는 `null`로 유지한다. 이 조회로 기존 DB 내용을 수정하지 않는다.
- 기존 Hibernate `ddl-auto: update` 배포 설정이 nullable `ai_recommendation.subject` 열을 추가한다. 새 행은 생성 시간을 자동 기록하며 생성 API는 DB 저장 완료 후 응답한다.
- `/api/counseling`의 생성 API에도 같은 학생 권한을 적용해 생성 경로를 통한 타 학생 정보 접근을 차단한다.

검증: 실제 JWT/H2 통합 테스트와 `./gradlew check bootJar --no-daemon`.
