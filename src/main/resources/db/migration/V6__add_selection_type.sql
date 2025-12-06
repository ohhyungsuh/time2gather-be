-- Add selection_type column to meetings and meeting_user_selections tables
-- This enables support for both time-based and all-day date selections

-- Add selection_type to meetings table
ALTER TABLE meetings
ADD COLUMN selection_type VARCHAR(20) NOT NULL DEFAULT 'TIME' COMMENT '선택 타입: TIME(시간 단위), ALL_DAY(일 단위)';

-- Add selection_type to meeting_user_selections table
ALTER TABLE meeting_user_selections
ADD COLUMN selection_type VARCHAR(20) NOT NULL DEFAULT 'TIME' COMMENT '선택 타입: TIME(시간 단위), ALL_DAY(일 단위)';

-- All existing data will have 'TIME' as the default value
-- This ensures backward compatibility with existing meetings

