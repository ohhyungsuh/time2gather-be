-- V9: 장소 투표 기능 추가
-- 1. meetings 테이블 확장 (장소 투표 활성화, 확정된 장소)
-- 2. meeting_locations 테이블 (장소 후보)
-- 3. meeting_location_selections 테이블 (장소 투표)

-- meetings 테이블 확장
ALTER TABLE meetings ADD COLUMN location_vote_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE meetings ADD COLUMN confirmed_location_id BIGINT NULL;
ALTER TABLE meetings ADD COLUMN location_confirmed_at DATETIME NULL;

-- 장소 후보 테이블
CREATE TABLE meeting_locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_meeting_locations_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE,
    INDEX idx_meeting_locations_meeting_id (meeting_id)
);

-- 장소 투표 테이블
CREATE TABLE meeting_location_selections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_location_selections_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE,
    CONSTRAINT fk_location_selections_location FOREIGN KEY (location_id) REFERENCES meeting_locations(id) ON DELETE CASCADE,
    CONSTRAINT unique_location_vote UNIQUE (meeting_id, location_id, user_id),
    INDEX idx_location_selections_meeting_id (meeting_id),
    INDEX idx_location_selections_location_id (location_id),
    INDEX idx_location_selections_user_id (user_id)
);

-- meetings 테이블에 confirmed_location_id FK 추가 (meeting_locations 생성 후)
ALTER TABLE meetings ADD CONSTRAINT fk_meetings_confirmed_location 
    FOREIGN KEY (confirmed_location_id) REFERENCES meeting_locations(id) ON DELETE SET NULL;
