package com.usst.meetingroom.dto;

import java.time.LocalDateTime;

public record BookingView(
        Long id,
        String bookingNo,
        Long userId,
        String applicantName,
        Long roomId,
        String roomName,
        String buildingName,
        String location,
        Integer capacity,
        String subject,
        String purpose,
        Integer attendeeCount,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        String statusText,
        String cancelReason,
        String approvalComment,
        String approverName,
        LocalDateTime createdAt
) {
}

