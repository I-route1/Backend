import board
import busio
from digitalio import DigitalInOut
from adafruit_pn532.spi import PN532_SPI
import requests
import time

# ──────────────────────────────────────────────
# 설정
# ──────────────────────────────────────────────
BACKEND_URL = "https://demystify-handcuff-protegee.ngrok-free.dev"
BUS_ID = 1

# admin 또는 teacher 계정 사용 가능 (둘 다 NFC 등록 권한 있음)
LOGIN_USERNAME = "admin"
LOGIN_PASSWORD = "Admin1234!"
# LOGIN_USERNAME = "teacher"
# LOGIN_PASSWORD = "Teacher1234!"
# ──────────────────────────────────────────────


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
