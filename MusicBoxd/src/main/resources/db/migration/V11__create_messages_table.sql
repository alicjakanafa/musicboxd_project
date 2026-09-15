CREATE TABLE messages(
    id SERIAL PRIMARY KEY,
    sender_id BIGINT,
    receiver_id BIGINT,
    content TEXT,
    read BOOLEAN,
    song_title VARCHAR(255),
    song_artist VARCHAR(255),
    song_image_url VARCHAR(500),
    song_preview_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);