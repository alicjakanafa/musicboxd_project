CREATE TABLE notifications (
   id bigserial PRIMARY KEY,
   user_id BIGINT,
   actor_id BIGINT,
   related_id BIGINT,
   type VARCHAR(50),
   notification_text VARCHAR(255),
   is_read BOOLEAN DEFAULT FALSE,
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);