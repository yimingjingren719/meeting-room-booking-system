package com.usst.meetingroom.dto;

import java.time.LocalDateTime;

public record AuditLogView(
        Long id,
        String operatorName,
        String action,
        String targetType,
        String targetId,
        String result,
        String detail,
        LocalDateTime createdAt
) {
}

