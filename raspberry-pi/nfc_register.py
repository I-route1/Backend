import board
import busio
import time
from digitalio import DigitalInOut
from adafruit_pn532.spi import PN532_SPI
import requests

# ──────────────────────────────────────────────
# 설정
# ──────────────────────────────────────────────
BACKEND_URL = "https://demystify-handcuff-protegee.ngrok-free.dev"
ADMIN_USERNAME = "your_admin_username"
ADMIN_PASSWORD = "your_admin_password"
POLL_INTERVAL = 1  # 폴링 간격 (초)
# ──────────────────────────────────────────────


def login(username, password):
    resp = requests.post(
        f"{BACKEND_URL}/api/auth/login",
        json={"username": username, "password": password},
        timeout=5
    )
    resp.raise_for_status()
    return resp.json()["accessToken"]


def poll_pending(token):
    """등록 대기 중인 학생 ID 조회. 없으면 None 반환."""
    resp = requests.get(
        f"{BACKEND_URL}/api/gps/nfc/pending",
        headers={"Authorization": f"Bearer {token}"},
        timeout=5
    )
    if resp.status_code == 204:
        return None
    resp.raise_for_status()
    return resp.json()["studentId"]


def register_nfc(student_id, nfc_card_id, token):
    resp = requests.patch(
        f"{BACKEND_URL}/api/gps/students/{student_id}/nfc",
        json={"nfcCardId": nfc_card_id},
        headers={"Authorization": f"Bearer {token}"},
        timeout=5
    )
    resp.raise_for_status()
    print(f"[등록 완료] 학생 {student_id}번 → NFC 카드 {nfc_card_id}")


if __name__ == "__main__":
    spi = busio.SPI(board.SCK, board.MOSI, board.MISO)
    cs_pin = DigitalInOut(board.CE0)
    pn532 = PN532_SPI(spi, cs_pin, debug=False)
    pn532.SAM_configuration()

    print("백엔드 로그인 중...")
    try:
        token = login(ADMIN_USERNAME, ADMIN_PASSWORD)
    except Exception as e:
        print(f"로그인 실패: {e}")
        exit(1)
    print("로그인 완료. 프론트에서 등록 요청을 기다리는 중...")

    while True:
        try:
            student_id = poll_pending(token)
            if student_id:
                print(f"학생 {student_id}번 NFC 카드를 갖다 대주세요...")
                while True:
                    uid = pn532.read_passive_target(timeout=0.5)
                    if uid:
                        nfc_card_id = uid.hex().upper()
                        print(f"카드 UID: {nfc_card_id}")
                        try:
                            register_nfc(student_id, nfc_card_id, token)
                        except requests.HTTPError as e:
                            print(f"등록 실패: {e.response.status_code} - {e.response.text}")
                        break
            else:
                time.sleep(POLL_INTERVAL)
        except requests.RequestException as e:
            print(f"서버 연결 오류: {e}")
            time.sleep(POLL_INTERVAL)
