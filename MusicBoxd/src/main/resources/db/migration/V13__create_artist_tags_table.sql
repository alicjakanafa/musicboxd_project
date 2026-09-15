CREATE TABLE artist_tags(
    artist_id BIGINT REFERENCES artists(id),
    tag_id BIGINT REFERENCES tags(id),
    PRIMARY KEY (artist_id, tag_id)
);