-- PN532 모듈 교체에 따른 rfid_card_id → nfc_card_id 컬럼 마이그레이션
-- MySQL 8.0+ 기준
-- 실행 전 반드시 백업 후 진행

ALTER TABLE student
    CHANGE COLUMN rfid_card_id nfc_card_id VARCHAR(20) UNIQUE COMMENT 'PN532 NFC 카드 UID (최대 7바이트, 14자리 hex)';
