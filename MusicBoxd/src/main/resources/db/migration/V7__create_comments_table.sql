CREATE TABLE comments (
     id bigserial PRIMARY KEY,
     user_id BIGINT,
     review_id BIGINT,
     content TEXT,
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);