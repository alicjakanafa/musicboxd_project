CREATE TABLE want_to_listen(
    id SERIAL PRIMARY KEY,
    user_id BIGINT,
    song_id BIGINT,
    album_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);