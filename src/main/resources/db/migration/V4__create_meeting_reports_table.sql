CREATE TABLE meeting_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id BIGINT NOT NULL UNIQUE,
    summary_text TEXT NOT NULL COMMENT 'AI가 생성한 모임 요약 레포트',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '재시도 횟수',
    last_attempt_at TIMESTAMP NULL COMMENT '마지막 시도 시각',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE,
    INDEX idx_meeting_id (meeting_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
