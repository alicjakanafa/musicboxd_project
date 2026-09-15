CREATE TABLE album_tags(
    album_id BIGINT REFERENCES albums(id),
    tag_id BIGINT REFERENCES tags(id),
    PRIMARY KEY (album_id, tag_id)
);