package com.usst.meetingroom.dto;

import java.time.LocalDateTime;

public record NotificationView(
        Long id,
        Long bookingId,
        String title,
        String content,
        Boolean read,
        LocalDateTime createdAt
) {
}

