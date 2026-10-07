package com.usst.meetingroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UnavailableRequest(
        @NotNull(message = "请选择会议室")
        Long roomId,

        @NotNull(message = "请选择开始时间")
        LocalDateTime startTime,

        @NotNull(message = "请选择结束时间")
        LocalDateTime endTime,

        @NotBlank(message = "请输入维护原因")
        @Size(max = 200, message = "维护原因不能超过 200 个字符")
        String reason
) {
}

