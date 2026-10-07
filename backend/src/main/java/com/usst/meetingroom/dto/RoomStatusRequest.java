package com.usst.meetingroom.dto;

import jakarta.validation.constraints.NotBlank;

public record RoomStatusRequest(
        @NotBlank(message = "请选择会议室状态")
        String status
) {
}
