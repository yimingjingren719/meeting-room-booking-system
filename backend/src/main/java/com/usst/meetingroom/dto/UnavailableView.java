package com.usst.meetingroom.dto;

import java.time.LocalDateTime;

public record UnavailableView(
        Long id,
        Long roomId,
        String roomName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String reason,
        LocalDateTime createdAt
) {
}

