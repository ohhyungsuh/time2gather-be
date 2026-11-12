CREATE TABLE meeting_names (
    id VARCHAR(255) PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL,
    UNIQUE KEY uk_meeting_name (meeting_id, user_id),
    FOREIGN KEY (meeting_id) REFERENCES meetings(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
