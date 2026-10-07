CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    display_name VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_user_username UNIQUE (username)
);

CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(50) NOT NULL,
    CONSTRAINT uk_sys_role_code UNIQUE (code)
);

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
);

CREATE TABLE building (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    campus VARCHAR(80) NOT NULL,
    address VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    floor_no INT NOT NULL,
    location VARCHAR(120) NOT NULL,
    capacity INT NOT NULL,
    open_time TIME NOT NULL DEFAULT '08:00:00',
    close_time TIME NOT NULL DEFAULT '21:00:00',
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    image_url VARCHAR(300),
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_room_building FOREIGN KEY (building_id) REFERENCES building (id)
);

CREATE TABLE equipment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    icon VARCHAR(50),
    CONSTRAINT uk_equipment_name UNIQUE (name)
);

CREATE TABLE room_equipment (
    room_id BIGINT NOT NULL,
    equipment_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    PRIMARY KEY (room_id, equipment_id),
    CONSTRAINT fk_room_equipment_room FOREIGN KEY (room_id) REFERENCES room (id),
    CONSTRAINT fk_room_equipment_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id)
);

CREATE TABLE room_unavailable (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    reason VARCHAR(200) NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_unavailable_room FOREIGN KEY (room_id) REFERENCES room (id),
    CONSTRAINT fk_unavailable_user FOREIGN KEY (created_by) REFERENCES sys_user (id)
);

CREATE TABLE booking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_no VARCHAR(40) NOT NULL,
    user_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    subject VARCHAR(120) NOT NULL,
    purpose VARCHAR(500),
    attendee_count INT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL,
    cancel_reason VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_booking_no UNIQUE (booking_no),
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_booking_room FOREIGN KEY (room_id) REFERENCES room (id)
);

CREATE TABLE booking_approval (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    result VARCHAR(20) NOT NULL,
    comment VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_booking_approval_booking UNIQUE (booking_id),
    CONSTRAINT fk_approval_booking FOREIGN KEY (booking_id) REFERENCES booking (id),
    CONSTRAINT fk_approval_user FOREIGN KEY (approver_id) REFERENCES sys_user (id)
);

CREATE TABLE notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    booking_id BIGINT,
    title VARCHAR(120) NOT NULL,
    content VARCHAR(500) NOT NULL,
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_user FOREIGN KEY (recipient_id) REFERENCES sys_user (id),
    CONSTRAINT fk_notification_booking FOREIGN KEY (booking_id) REFERENCES booking (id)
);

CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id BIGINT,
    action VARCHAR(60) NOT NULL,
    target_type VARCHAR(60) NOT NULL,
    target_id VARCHAR(60),
    result VARCHAR(20) NOT NULL,
    detail VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (operator_id) REFERENCES sys_user (id)
);

CREATE INDEX idx_room_building_status ON room (building_id, status);
CREATE INDEX idx_room_capacity ON room (capacity);
CREATE INDEX idx_unavailable_room_time ON room_unavailable (room_id, start_time, end_time);
CREATE INDEX idx_booking_room_time ON booking (room_id, start_time, end_time);
CREATE INDEX idx_booking_user_time ON booking (user_id, start_time);
CREATE INDEX idx_booking_status ON booking (status);
CREATE INDEX idx_notification_recipient_read ON notification (recipient_id, read_flag);
CREATE INDEX idx_audit_created_at ON audit_log (created_at);
