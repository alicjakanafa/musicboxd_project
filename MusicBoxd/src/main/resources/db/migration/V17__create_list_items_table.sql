CREATE TABLE list_items(
  id SERIAL PRIMARY KEY,
  list_id BIGINT,
  album_id BIGINT,
  song_id BIGINT,
  position INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);