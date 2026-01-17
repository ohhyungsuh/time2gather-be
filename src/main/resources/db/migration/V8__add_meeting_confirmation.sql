-- 미팅 확정 기능을 위한 컬럼 추가
ALTER TABLE meetings ADD COLUMN confirmed_date DATE NULL;
ALTER TABLE meetings ADD COLUMN confirmed_slot_index INT NULL;
ALTER TABLE meetings ADD COLUMN confirmed_at DATETIME NULL;
