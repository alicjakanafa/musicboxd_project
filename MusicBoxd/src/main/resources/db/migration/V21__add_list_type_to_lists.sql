ALTER TABLE lists
    ADD COLUMN list_type VARCHAR(50) NOT NULL DEFAULT 'CUSTOM';

ALTER TABLE lists
    ADD CONSTRAINT lists_list_type_check
        CHECK (list_type IN ('CUSTOM', 'WANT_TO_LISTEN', 'FAVOURITES'));