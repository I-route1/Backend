# i-Route 배포 가이드

## 인프라 구성

| 서비스 | 주소 | 비고 |
|--------|------|------|
| **프론트엔드** | https://d3kh3x870d7dr4.cloudfront.net | AWS S3 + CloudFront |
| **백엔드** | https://d22mlgf6je9oud.cloudfront.net | AWS EC2 + CloudFront |
| **AI 서버** | ngrok URL (재시작 시 변경) | 로컬 PC에서 실행 |

---

## 자동 배포 (GitHub Actions)

### 백엔드
- `develop` 브랜치에 push하면 **자동으로 EC2에 배포**됩니다.
- 빌드 → JAR 전송 → EC2 서버 재시작까지 자동으로 진행됩니다.

### 프론트엔드
- `main` 브랜치에 push하면 **자동으로 S3에 배포**됩니다.
- 빌드 → S3 업로드 → CloudFront 캐시 무효화까지 자동으로 진행됩니다.

---

## AI 서버 시작 방법 (김우주 PC에서만 실행)

AI 서버는 RTX 5070 Ti GPU가 필요하므로 WOOJOO PC에서만 실행합니다.

PC 켤 때마다 아래 스크립트를 실행해주세요:

```powershell
C:\Users\User\IdeaProjects\AI\start-ai.ps1
```

이 스크립트는 자동으로:
1. AI 서버 시작 (포트 8082)
2. ngrok HTTPS 터널 발급
3. EC2 백엔드에 AI 서버 URL 업데이트 및 재시작

---

## 테스트 계정

| 역할 | 아이디 | 비밀번호 |
|------|--------|----------|
| 학부모 | frontdev | Test1234! |
| 관리자 | admin | Admin1234! |
| 학원 | teacher | Teacher1234! |
| 기사 | driver | Driver1234! |
| 크레딧없음 | nocredit | Test1234! |

---

## EC2 서버 수동 관리 (필요한 경우)

```bash
# SSH 접속
ssh -i i-route-key.pem ec2-user@3.39.11.129

# 서버 로그 확인
tail -f /home/ec2-user/server.log

# 서버 재시작
/home/ec2-user/start.sh

# 서버 종료
pkill -f 'java -Dfile'
```

---

## GitHub Secrets 목록

배포에 필요한 Secrets은 이미 GitHub에 등록되어 있습니다.
추가/변경이 필요하면 각 레포의 Settings → Secrets and variables → Actions에서 수정하세요.

**Backend 레포 Secrets:**
- `EC2_HOST`, `EC2_KEY`, `JWT_SECRET`, `KAKAO_CLIENT_ID`, `KAKAO_REST_API_KEY`, `AI_SERVER_URL`

**Frontend 레포 Secrets:**
- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `S3_BUCKET`, `CF_DIST_ID`
- `VITE_API_URL`, `VITE_WS_URL`, `VITE_KAKAO_MAP_KEY`, `VITE_TOSS_CLIENT_KEY`
