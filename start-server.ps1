# i-Route 백엔드 서버 기동 스크립트
# 사용법: .\start-server.ps1

# ── 1. Redis (WSL) ────────────────────────────────────────────────
Write-Host "Redis 상태 확인 중..." -ForegroundColor Cyan
$redisPing = wsl -e bash -c "redis-cli ping 2>/dev/null"
if ($redisPing -eq "PONG") {
    Write-Host "  Redis 이미 실행 중." -ForegroundColor DarkGray
} else {
    Write-Host "  Redis 시작 중..." -ForegroundColor Yellow
    wsl -e bash -c "redis-server --daemonize yes --port 6379" | Out-Null
    Start-Sleep -Seconds 2
    $redisPing = wsl -e bash -c "redis-cli ping 2>/dev/null"
    if ($redisPing -eq "PONG") {
        Write-Host "  Redis 기동 완료." -ForegroundColor Green
    } else {
        Write-Host "  Redis 기동 실패. 계속 진행하지만 GPS 기능이 동작하지 않을 수 있습니다." -ForegroundColor Red
    }
}

# ── 2. 환경변수 ───────────────────────────────────────────────────
$env:JWT_SECRET          = "aXJvdXRlLXN1cGVyLXNlY3JldC1rZXktZm9yLWp3dC10b2tlbi1zaWduaW5n"
$env:I_ROUTE_DB_USERNAME = "iroute"
$env:I_ROUTE_DB_PASSWORD = "iroute_pw"
$env:MAIL_USERNAME       = "dev@example.com"
$env:MAIL_PASSWORD       = "dev-password"
$env:KAKAO_CLIENT_ID     = "3eb63a2338882943f3b2280a3460d9c8"
$env:KAKAO_REST_API_KEY  = "3eb63a2338882943f3b2280a3460d9c8"
$env:KAKAO_REDIRECT_URI  = "http://localhost:8080/api/auth/kakao/callback"
$env:AI_SERVER_URL       = "http://localhost:8082"

Write-Host "환경변수 설정 완료. 서버 기동 중..." -ForegroundColor Cyan

# ── 3. Spring Boot 서버 ───────────────────────────────────────────
Start-Process -NoNewWindow -FilePath ".\gradlew.bat" -ArgumentList "bootRun" `
    -WorkingDirectory (Get-Location) `
    -RedirectStandardOutput ".\server.log" `
    -RedirectStandardError ".\server-err.log"

# 서버 기동 대기 — 어떤 HTTP 응답이든 오면 기동 완료로 판단
Write-Host "서버 준비 대기 중..." -ForegroundColor Yellow
$ready = $false
for ($i = 0; $i -lt 24; $i++) {
    Start-Sleep -Seconds 5
    try {
        Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" `
            -Method Post -ContentType "application/json" `
            -Body '{"username":"x","password":"x"}' `
            -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop | Out-Null
    } catch {
        if ($_.Exception.Response -ne $null) { $ready = $true; break }
    }
    Write-Host "  대기 중... ($($i+1)/24)" -ForegroundColor DarkGray
}

if (-not $ready) {
    Write-Host "서버 기동 실패 또는 시간 초과. server.log 확인 필요." -ForegroundColor Red
    exit 1
}

Write-Host "서버 기동 완료!" -ForegroundColor Green

# ── 4. 테스트 계정 토큰 ───────────────────────────────────────────
# 로그인 API 필드: username (이메일 아님)
# - PARENT  : username=frontdev  (email=frontend@iroute.dev)
# - ADMIN   : username=admin     (email=admin@iroute.dev)
# - ACADEMY : username=teacher   (email=teacher@iroute.dev)
# - NO_PREM : username=nocredit  (email=nocredit@test.com)
$accounts = @(
    @{ label = "PARENT  (frontend@iroute.dev / userId=14)"; username = "frontdev"; password = "Test1234!" },
    @{ label = "ADMIN   (admin@iroute.dev    / userId=15)"; username = "admin";    password = "Admin1234!" },
    @{ label = "ACADEMY (teacher@iroute.dev  / userId=17)"; username = "teacher";  password = "Teacher1234!" },
    @{ label = "NO_PREM (nocredit@test.com   / userId=18)"; username = "nocredit"; password = "Test1234!" }
)

Write-Host "`n===== 테스트 계정 토큰 =====" -ForegroundColor Cyan

foreach ($acc in $accounts) {
    $body = '{"username":"' + $acc.username + '","password":"' + $acc.password + '"}'
    try {
        $res = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
            -Method Post -ContentType "application/json" -Body $body -ErrorAction Stop
        Write-Host "`n[$($acc.label)]" -ForegroundColor Yellow
        Write-Host "  accessToken : $($res.accessToken)"
        Write-Host "  refreshToken: $($res.refreshToken)"
    } catch {
        Write-Host "`n[$($acc.label)] 토큰 발급 실패: $_" -ForegroundColor Red
    }
}

Write-Host "`n============================`n" -ForegroundColor Cyan

# ── 5. GPS 시뮬레이터 ─────────────────────────────────────────────
# busId=1 (BUS-001): stop-A(35.8468, 128.6132) ↔ stop-B(35.85, 128.62) 왕복
# 5초마다 위치 전송 (기사 앱 대역)
$sim = Start-Job -Name "GpsSimulator" -ScriptBlock {
    $busId = 1
    $stops = @(
        @{ lat = 35.8468; lon = 128.6132 },
        @{ lat = 35.8500; lon = 128.6200 }
    )
    $steps = 20; $direction = 1; $step = 0
    while ($true) {
        $from = if ($direction -eq 1) { $stops[0] } else { $stops[1] }
        $to   = if ($direction -eq 1) { $stops[1] } else { $stops[0] }
        $t   = $step / $steps
        $lat = [Math]::Round($from.lat + ($to.lat - $from.lat) * $t, 6)
        $lon = [Math]::Round($from.lon + ($to.lon - $from.lon) * $t, 6)
        $heading = [Math]::Round(([Math]::Atan2($to.lon - $from.lon, $to.lat - $from.lat) * 180 / [Math]::PI + 360) % 360, 1)
        $body = "{`"busId`":$busId,`"latitude`":$lat,`"longitude`":$lon,`"speed`":30.0,`"heading`":$heading}"
        try {
            Invoke-RestMethod -Uri "http://localhost:8080/api/gps/locations" `
                -Method Post -ContentType "application/json" -Body $body -TimeoutSec 3 | Out-Null
        } catch {}
        $step++
        if ($step -gt $steps) { $step = 0; $direction = -$direction }
        Start-Sleep -Seconds 5
    }
}
Write-Host "GPS 시뮬레이터 시작 (Job ID: $($sim.Id)) — BUS-001 stop-A↔stop-B 왕복, 5초 간격" -ForegroundColor Green
Write-Host "종료하려면: Stop-Job -Name GpsSimulator`n" -ForegroundColor DarkGray
