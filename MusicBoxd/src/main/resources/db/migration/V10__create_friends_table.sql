CREATE TABLE friends (
    id SERIAL PRIMARY KEY,
    requester_id BIGINT,
    receiver_id BIGINT,
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);