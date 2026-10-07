package com.usst.meetingroom.dto;

import jakarta.validation.constraints.NotNull;

public record BookingPermissionRequest(
        @NotNull(message = "请选择预约权限")
        Boolean allowed,
        String reason
) {
}

