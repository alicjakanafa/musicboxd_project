CREATE TABLE reviews (
    id bigserial PRIMARY KEY,
    user_id BIGINT,
    album_id BIGINT,
    song_id BIGINT,
    header VARCHAR(100),
    content TEXT,
    Rating DECIMAL(2,1),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);