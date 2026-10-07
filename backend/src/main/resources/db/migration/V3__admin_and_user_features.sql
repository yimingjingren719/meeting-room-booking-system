ALTER TABLE sys_user ADD COLUMN booking_allowed BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE sys_user ADD COLUMN booking_block_reason VARCHAR(200);

CREATE TABLE user_favorite_room (
    user_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, room_id),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_favorite_room FOREIGN KEY (room_id) REFERENCES room (id)
);

CREATE INDEX idx_favorite_room ON user_favorite_room (room_id);

