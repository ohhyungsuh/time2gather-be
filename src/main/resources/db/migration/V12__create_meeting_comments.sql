CREATE TABLE meeting_comments (
    id VARCHAR(36) PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    root_id VARCHAR(36),
    reply_to_id VARCHAR(36),
    body VARCHAR(1000) NOT NULL,
    mentions JSON,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    edited BOOLEAN NOT NULL,
    status VARCHAR(12) NOT NULL,
    request_id VARCHAR(36) NOT NULL,
    INDEX idx_comment_meeting (meeting_id),
    UNIQUE KEY uk_comment_request (meeting_id, author_id, request_id),
    FOREIGN KEY (meeting_id) REFERENCES meetings(id),
    FOREIGN KEY (author_id) REFERENCES users(id)
);
