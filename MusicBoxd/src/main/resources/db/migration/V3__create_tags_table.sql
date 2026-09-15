TRUNCATE TABLE IF EXISTS tags;

CREATE TABLE tags (
     id bigserial PRIMARY KEY,
     name VARCHAR(255);
);