CREATE TABLE lists(
    id SERIAL PRIMARY KEY,
    user_id BIGINT,
    title BIGINT,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);