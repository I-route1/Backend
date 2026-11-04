# i-Route 배포 가이드

## 인프라 구성 (2026-10-01, AWS 계정 207208119870, 서울 리전)

| 서비스 | 주소 | 구성 |
|--------|------|------|
| **프론트엔드** | https://d2nos5u98g310z.cloudfront.net | S3 `i-route-front-207208119870` + CloudFront `E2963NB69BRUTF` |
| **백엔드** | https://d2t8h2oy220lpg.cloudfront.net | EC2 `i-route-backend`(t3.small, 탄력적 IP 13.124.201.1) + CloudFront `E24LS0TB4AATL3` |
| **AI 서버** | http://100.125.153.53:8082 (Tailscale) | 김우주 PC(RTX 5070 Ti)에서 실행 |

- 백엔드 CloudFront는 EC2의 8080으로 HTTP 연결하고, 원본 응답 제한은 60초, 캐시는 끕니다.
  개념 추천의 AI 대기 한도(50초, `AiReportController.AI_TIMEOUT`)가 이 60초보다 짧아야 합니다.
- EC2 보안 그룹은 22(배포용 SSH)와 8080(CloudFront 접두사 목록에서만)만 엽니다.
- EC2와 AI PC는 같은 Tailscale 네트워크에 있습니다. AI 서버는 인터넷에 열려 있지 않습니다.

---

## 자동 배포 (GitHub Actions)

### 백엔드 (`develop` push 또는 Actions 탭에서 수동 실행)
배포 스크립트 테스트 → 빌드 → GitHub Secrets의 메일·카카오 Admin Key를 서버에 전달 →
SMTP 인증 확인(메일 발송 없음) → `start.sh`로 재시작 → `/health`의 200/OK와 Java 프로세스의 설정 일치를 확인합니다.
실패하면 이전 JAR·설정·시작 스크립트를 복구하고 재시작합니다. 동시 배포는 순서대로 처리합니다.

### 프론트엔드 (`main` push 또는 Actions 탭에서 수동 실행)
빌드 → S3 업로드(`--delete`) → CloudFront 캐시 무효화.

---

## AI 서버 시작 (김우주 PC)

Tailscale이 Connected인지 확인한 뒤 실행합니다.

```powershell
C:\Users\User\IdeaProjects\AI\start-ai.ps1
```

모델 로드에 1~2분 걸리고, 뜨면 `http://100.125.153.53:8082`를 출력합니다.
주소가 고정이라 EC2 쪽은 손대지 않아도 됩니다. Windows 방화벽은 8082를
Tailscale 대역(100.64.0.0/10)에서만 허용합니다.

---

## 테스트 계정

| 역할 | 아이디 | 비밀번호 |
|------|--------|----------|
| 학부모 | frontdev | `$TEST_PARENT_PASSWORD` |
| 관리자 | admin | `$TEST_ADMIN_PASSWORD` |
| 학원 | teacher | `$TEST_ACADEMY_PASSWORD` |
| 기사 | driver | `$TEST_DRIVER_PASSWORD` |
| 크레딧없음 | nocredit | `$TEST_NOPREM_PASSWORD` |

비밀번호는 로컬 `.env`에 있습니다 (`.env.example` 참고). 이 저장소는 공개
저장소라 문서에 평문으로 적지 않습니다.

---

## EC2 서버 관리

DB·JWT·AI 주소 등 기존 설정은 EC2 `/home/ec2-user/start.sh`에서 유지합니다.
메일·카카오 Admin Key는 GitHub Secrets에서 배포 시 `/home/ec2-user/.i_route_runtime.env`로
전달하며 파일 권한은 600입니다. `start.sh`가 Java 실행 직전에 이를 읽어 이전 값을 덮어씁니다.
변경할 때는 GitHub Secrets를 수정하고 재배포하세요. DB 비밀번호는 `~/.i_route_db_pw`에서 읽습니다.
배포 직전 백업은 권한 700의 `~/.deploy/<실행 ID>-<시도>/` 안에 보관하며 비밀값을 포함할 수 있으므로 공개하지 않습니다.

```bash
# SSH 접속 (키는 저장소에 두지 않습니다)
ssh -i ~/Downloads/i-route-key.pem ec2-user@13.124.201.1

# 서버 로그
tail -f /home/ec2-user/server.log

# 재시작 (기존 java를 끄고 다시 띄움). EC2를 재부팅한 뒤에도 이걸 실행해야 합니다.
bash /home/ec2-user/start.sh

# AI 서버 연결 확인 (200이면 정상)
curl -s -o /dev/null -w '%{http_code}\n' http://100.125.153.53:8082/docs
```

EC2에는 Java 17, MariaDB(`i_route_db`), Redis, 스왑 2GB, Tailscale(`i-route-backend`)이 설치돼 있습니다.

---

## GitHub Secrets

**Backend 레포** — 필수 Secrets: `EC2_HOST`, `EC2_KEY`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `KAKAO_ADMIN_KEY`.
메일/관리자 키가 비어 있으면 서버를 변경하기 전에 중단합니다. 실제 값은 로그에 출력하지 않습니다.
`JWT_SECRET`, 기타 `KAKAO_*`, `AI_SERVER_URL`은 기존 EC2 설정을 그대로 사용합니다.

**Frontend 레포**
- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` — IAM 사용자 `github-front-deploy`(정책 `i-route-front-deploy`: 버킷 업로드와 캐시 무효화만)
- `S3_BUCKET`, `CF_DIST_ID`
- `VITE_API_URL`, `VITE_WS_URL`, `VITE_KAKAO_MAP_KEY`, `VITE_KAKAO_CLIENT_ID`, `VITE_KAKAO_REDIRECT_URI`, `VITE_TOSS_CLIENT_KEY`

Kakao Developers의 Redirect URI에는 `https://d2nos5u98g310z.cloudfront.net/oauth/kakao/callback`이
등록돼 있어야 합니다(`VITE_KAKAO_REDIRECT_URI`, EC2 `KAKAO_REDIRECT_URI`와 같은 값).
