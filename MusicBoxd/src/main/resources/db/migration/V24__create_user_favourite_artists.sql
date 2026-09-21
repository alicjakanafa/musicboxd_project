CREATE TABLE USER_FAVOURITE_ARTISTS (
                                        id BIGSERIAL PRIMARY KEY,

                                        user_id BIGINT NOT NULL,

                                        artist_id BIGINT NOT NULL,

                                        CONSTRAINT fk_user_favourite_artist_user
                                            FOREIGN KEY (user_id)
                                                REFERENCES USERS(id)
                                                ON DELETE CASCADE,

                                        CONSTRAINT fk_user_favourite_artist_artist
                                            FOREIGN KEY (artist_id)
                                                REFERENCES ARTISTS(id)
                                                ON DELETE CASCADE,

                                        CONSTRAINT unique_user_favourite_artist
                                            UNIQUE (user_id, artist_id)
);