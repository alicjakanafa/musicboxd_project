TRUNCATE TABLE IF EXISTS albums;

CREATE TABLE albums (
   id bigserial PRIMARY KEY,
   external_id VARCHAR(255),
   artist_id BIGINT,
   title varchar(255),
   release_year YEAR,
   artwork_url TEXT,
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
);