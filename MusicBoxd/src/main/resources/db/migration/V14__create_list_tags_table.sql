CREATE TABLE list_tags(
  list_id BIGINT REFERENCES lists(id),
  tag_id BIGINT REFERENCES tags(id),
  PRIMARY KEY (list_id, tag_id)
);