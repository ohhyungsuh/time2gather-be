-- Meeting 테이블에 interval_minutes 컬럼 추가 (이미 있으면 무시)
ALTER TABLE meetings
ADD COLUMN IF NOT EXISTS interval_minutes INT NOT NULL DEFAULT 30 COMMENT '시간 슬롯 간격 (분)';

-- MeetingUserSelection 테이블에 interval_minutes 컬럼 추가 (이미 있으면 무시)
ALTER TABLE meeting_user_selections
ADD COLUMN IF NOT EXISTS interval_minutes INT NOT NULL DEFAULT 30 COMMENT '시간 슬롯 간격 (분)';

