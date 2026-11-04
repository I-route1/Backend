## 백엔드 개발 환경 (Environment)

오답·OCR 채점·집중도 저장/조회: [API 명세](docs/learning-records-api.md)

| 항목 | 버전           | 
|------|--------------|
| **Java** | 17           |
| **JDK** | 22.0.2       | 
| **Spring Boot** | 3.5.6        | 
| **Gradle** | 9.4.1 |
| **MySQL** | 8.4.7          |
| **Docker** | ...          |
---

## 실행 방법

```bash
# 1. 레포지토리 복사
https://github.com/I-route1/Backend.git

# 2. 의존성 다운로드 및 빌드
./gradlew clean build

# 3. 서버 실행
./gradlew bootRun
```

## 테스트

```bash
./gradlew test                 # 전체 단위 테스트 + 학습 기록 통합 테스트
./gradlew clean check bootJar  # 전체 검증 후 배포 JAR 빌드
```

Windows에서는 `./gradlew` 대신 `.\gradlew.bat`을 사용합니다. Java 17이 필요합니다.
테스트 DB는 H2를 사용하고 AI HTTP 연동은 임시 로컬 서버로 검증하므로 별도의 AI·MySQL·Redis·SMTP 서버가 필요하지 않습니다.
결제와 메일 발송은 mock으로 처리합니다. 라즈베리파이 실기기 연동은 포함하지 않습니다.

결과는 `build/reports/tests/test/index.html`과 `build/reports/tests/learningRecordsTest/index.html`에서 확인할 수 있습니다.
배포 워크플로도 `check bootJar`를 실행하며 테스트 실패 시 배포하지 않습니다.
