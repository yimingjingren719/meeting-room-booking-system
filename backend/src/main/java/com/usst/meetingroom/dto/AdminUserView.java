package com.usst.meetingroom.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserView(
        Long id,
        String username,
        String displayName,
        String email,
        String phone,
        String status,
        Boolean bookingAllowed,
        String bookingBlockReason,
        List<String> roles,
        LocalDateTime createdAt
) {
}

