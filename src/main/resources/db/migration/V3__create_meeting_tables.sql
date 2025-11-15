-- 모임(Meeting) 테이블
CREATE TABLE meetings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_code VARCHAR(100) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    host_user_id BIGINT NOT NULL,
    timezone VARCHAR(50) DEFAULT 'Asia/Seoul',
    available_dates JSON NOT NULL COMMENT '날짜별 가능한 시간대 (slotIndex 배열)',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (host_user_id) REFERENCES users(id),
    INDEX idx_meeting_code (meeting_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 사용자 시간 선택(MeetingUserSelection) 테이블
CREATE TABLE meeting_user_selections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    selections JSON NOT NULL COMMENT '날짜별 선택한 시간대 (slotIndex 배열)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY unique_user_meeting (meeting_id, user_id),
    INDEX idx_meeting_id (meeting_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

