CREATE TABLE songs (
    id bigserial PRIMARY KEY,
    external_id VARCHAR(255),
    album_id BIGINT,
    title VARCHAR(255),
    track_number INT,
    song_url TEXT,
    song_image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);