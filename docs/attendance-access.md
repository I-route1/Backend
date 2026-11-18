# 출결 조회 권한

출결 조회는 `Authorization: Bearer <accessToken>` 인증이 필요하다. 미로그인·무효/만료 토큰은 401이며, GET/HEAD 모두 적용한다. 응답은 `Cache-Control: no-store`로 캐시하지 않는다.

| 경로 | 허용 범위 |
|---|---|
| `GET /api/gps/attendance/students/{studentId}` | 학부모는 본인 자녀, 학원은 소속 학생, 기사는 배정 버스의 학생, 운영자는 전체 |
| `GET /api/gps/attendance/parents/{parentId}` | 학부모 본인 ID 또는 운영자 |
| `GET /api/gps/attendance/buses/{busId}` | 학원은 소속 학생 기록만 반환, 기사는 배정 버스만, 운영자는 전체 |
| `GET /api/gps/buses/{busId}/passengers/today` | 승하차 상태를 포함하므로 버스 출결과 동일한 인증·소속 필터 적용 |

다른 가정/학원의 학생과 존재하지 않는 학생은 동일한 403을 반환해 학생 존재 여부를 구분하지 않는다. 잘못된 ID·날짜는 400이다. 학생/학부모 경로의 `date`를 생략하면 서버의 오늘 날짜를 사용한다.

학원이 한 버스 안의 다른 학원 학생 기록을 읽거나, 학부모 ID 경로로 다른 자녀 기록에 우회 접근할 수 없도록 같은 정책을 적용한다.

## NFC 승·하차 기록

`POST /api/gps/attendance`는 `busId`, `nfcCardId`, `scanId`를 받는다. Pi는 같은 카드를 계속 대고 있거나 잠깐 인식이 끊긴 동안 한 번만 전송한다. 한 번의 태그에 UUID 한 개를 생성하고 네트워크 오류로 재시도할 때는 같은 UUID를 사용한다. 서버는 같은 학생의 동일 UUID에 처음 저장된 기록을 반환한다. 새로운 UUID는 새 태그로 처리한다. UUID 없는 구버전 요청은 같은 학생·버스의 최근 기록으로부터 10초 이내에 반복되면 기존 기록을 반환한다.

미등록 카드는 404다. 존재하지 않는 버스나 학생의 배정과 다른 버스는 422다. 카드 재등록은 해당 학생의 `nfc_card_id`만 바꾸며, 다른 학생이 사용하는 UID는 409다. 기존 출결 기록의 `scan_id`는 비워 두며 새 기록에는 학생별 UUID 고유 제약을 적용한다.

`AttendanceAccessIntegrationTest`는 실제 JWT·H2로 익명/다른 가정/다른 학원/배정 기사/운영자, 날짜·ID 경계, GET/HEAD·CORS, 버스의 소속 학생 필터를 검증한다. 전체 검증: `./gradlew check bootJar --no-daemon`.
