CREATE TABLE likes (
    id bigserial PRIMARY KEY,
    user_id BIGINT,
    review_id BIGINT,
    comment_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);