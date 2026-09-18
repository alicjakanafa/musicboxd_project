CREATE TABLE user_favourite_albums (
                                       id BIGSERIAL PRIMARY KEY,
                                       user_id BIGINT NOT NULL,
                                       album_id BIGINT NOT NULL,
                                       position INTEGER NOT NULL,

                                       CONSTRAINT fk_favourite_user
                                           FOREIGN KEY (user_id)
                                               REFERENCES users(id),

                                       CONSTRAINT fk_favourite_album
                                           FOREIGN KEY (album_id)
                                               REFERENCES albums(id),

                                       CONSTRAINT unique_user_album
                                           UNIQUE (user_id, album_id),

                                       CONSTRAINT unique_user_position
                                           UNIQUE (user_id, position)
);