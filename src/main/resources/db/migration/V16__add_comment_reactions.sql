ALTER TABLE meeting_comments
    ADD COLUMN reactions JSON,
    ADD COLUMN activity_at BIGINT;
