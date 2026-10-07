package com.usst.meetingroom.dto;

import jakarta.validation.constraints.Size;

public record CancelBookingRequest(
        @Size(max = 200, message = "取消原因不能超过 200 个字符")
        String reason
) {
}

