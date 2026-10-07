package com.usst.meetingroom.dto;

import java.time.LocalTime;

public record AvailabilitySlot(
        LocalTime startTime,
        LocalTime endTime,
        String status,
        String statusText
) {
}

