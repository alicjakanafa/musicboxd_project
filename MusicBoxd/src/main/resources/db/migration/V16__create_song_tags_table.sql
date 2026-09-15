CREATE TABLE song_tags(
    song_id BIGINT REFERENCES songs(id),
    tag_id BIGINT REFERENCES tags(id),
    PRIMARY KEY (song_id, tag_id)
);