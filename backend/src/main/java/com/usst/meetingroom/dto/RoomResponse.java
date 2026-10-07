package com.usst.meetingroom.dto;

import java.time.LocalTime;
import java.util.List;

public record RoomResponse(
        Long id,
        String name,
        Long buildingId,
        String buildingName,
        String campus,
        Integer floorNo,
        String location,
        Integer capacity,
        LocalTime openTime,
        LocalTime closeTime,
        String status,
        String imageUrl,
        String description,
        List<String> equipment,
        List<AvailabilitySlot> availability,
        Boolean favorite,
        Integer recommendationScore
) {
}

