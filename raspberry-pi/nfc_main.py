"""NFC 출결 리더(라즈베리파이 + PN532).

설정은 환경변수 또는 이 파일과 같은 디렉터리의 .env에서 읽는다.
이 저장소는 공개 저장소라 주소·계정을 코드에 적지 않는다.

    BACKEND_URL      예) https://d22mlgf6je9oud.cloudfront.net
    NFC_USERNAME     NFC 등록 권한이 있는 계정 (admin 또는 teacher)
    NFC_PASSWORD
    BUS_ID           기본 1
"""
import os
import sys
import time
from pathlib import Path

import board
import busio
import requests
from adafruit_pn532.spi import PN532_SPI
from digitalio import DigitalInOut


def _load_dotenv(path: Path) -> None:
    if not path.is_file():
        return
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, val = line.partition("=")
        os.environ.setdefault(key.strip(), val.strip().strip('"').strip("'"))


_load_dotenv(Path(__file__).parent / ".env")

BACKEND_URL = (os.getenv("BACKEND_URL") or "").rstrip("/")
LOGIN_USERNAME = os.getenv("NFC_USERNAME")
LOGIN_PASSWORD = os.getenv("NFC_PASSWORD")
BUS_ID = int(os.getenv("BUS_ID", "1"))

_missing = [n for n, v in (("BACKEND_URL", BACKEND_URL),
                           ("NFC_USERNAME", LOGIN_USERNAME),
                           ("NFC_PASSWORD", LOGIN_PASSWORD)) if not v]
if _missing:
    print(f"설정이 없습니다: {', '.join(_missing)} — .env.example을 .env로 복사해 채우세요.")
    sys.exit(1)

# ngrok 터널을 쓰는 경우에만 필요한 헤더. 그 외 주소에서는 무시된다.
HEADERS = {"ngrok-skip-browser-warning": "true"}


def login(username, password):
    resp = requests.post(
        f"{BACKEND_URL}/api/auth/login",
        json={"username": username, "password": password},
        headers=HEADERS,
        timeout=5
    )
    resp.raise_for_status()
    return resp.json()["accessToken"]


def tag_attendance(nfc_card_id):
    return requests.post(
        f"{BACKEND_URL}/api/gps/attendance",
        json={"busId": BUS_ID, "nfcCardId": nfc_card_id},
        headers=HEADERS,
        timeout=5
    )


def register_nfc(student_id, nfc_card_id, token):
    resp = requests.patch(
        f"{BACKEND_URL}/api/gps/students/{student_id}/nfc",
        json={"nfcCardId": nfc_card_id},
        headers={**HEADERS, "Authorization": f"Bearer {token}"},
        timeout=5
    )
    resp.raise_for_status()
    print(f"[등록 완료] 학생 {student_id}번 → NFC 카드 {nfc_card_id}")


spi = busio.SPI(board.SCK, board.MOSI, board.MISO)
cs_pin = DigitalInOut(board.CE0)
pn532 = PN532_SPI(spi, cs_pin, debug=False)
pn532.SAM_configuration()

print("백엔드 로그인 중...")
try:
    token = login(LOGIN_USERNAME, LOGIN_PASSWORD)
except Exception as e:
    print(f"로그인 실패: {e}")
    exit(1)
print("NFC 리더 준비 완료. 카드를 갖다 대주세요...")

last_uid = None
last_tag_time = 0

while True:
    uid = pn532.read_passive_target(timeout=0.5)

    if uid is None:
        last_uid = None
        continue

    uid_hex = uid.hex().upper()
    now = time.time()
    if uid_hex == last_uid and now - last_tag_time < 3:
        continue

    last_uid = uid_hex
    last_tag_time = now

    print(f"카드 UID: {uid_hex}")

    try:
        resp = tag_attendance(uid_hex)

        if resp.status_code == 200:
            data = resp.json()
            event = "탑승" if data["eventType"] == "BOARD" else "하차"
            print(f"[{event}] {data['studentName']} - {data['timestamp']}")

        elif resp.status_code == 404:
            print("미등록 카드입니다. 등록할 학생 ID를 입력하세요: ", end="", flush=True)
            try:
                student_id = int(input())
                register_nfc(student_id, uid_hex, token)
            except ValueError:
                print("올바른 학생 ID를 입력하세요.")
            except requests.HTTPError as e:
                print(f"등록 실패: {e.response.status_code} - {e.response.text}")

        else:
            print(f"오류: {resp.status_code} - {resp.text}")

    except requests.exceptions.ConnectionError:
        print("서버 연결 실패")
    except Exception as e:
        print(f"오류 발생: {e}")

    time.sleep(0.5)
