-- Meeting 테이블에 interval_minutes 컬럼 추가
ALTER TABLE meetings
ADD COLUMN interval_minutes INT NOT NULL DEFAULT 30 COMMENT '시간 슬롯 간격 (분)';

-- MeetingUserSelection 테이블에 interval_minutes 컬럼 추가
ALTER TABLE meeting_user_selections
ADD COLUMN interval_minutes INT NOT NULL DEFAULT 30 COMMENT '시간 슬롯 간격 (분)';

