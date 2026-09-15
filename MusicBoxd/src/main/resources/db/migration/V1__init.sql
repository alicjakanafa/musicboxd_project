TRUNCATE TABLE IF EXISTS users;

CREATE TABLE users (
   id bigserial PRIMARY KEY,
   google_user_id VARCHAR(255) UNIQUE,
   username varchar(50) NOT NULL UNIQUE,
   email VARCHAR(255),
   bio TEXT,
   profile_picture_url TEXT,
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
);