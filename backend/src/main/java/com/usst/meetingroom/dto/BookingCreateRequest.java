package com.usst.meetingroom.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record BookingCreateRequest(
        @NotNull(message = "请选择会议室")
        Long roomId,

        @NotBlank(message = "请输入会议主题")
        @Size(max = 120, message = "会议主题不能超过 120 个字符")
        String subject,

        @Size(max = 500, message = "用途说明不能超过 500 个字符")
        String purpose,

        @NotNull(message = "请输入参会人数")
        @Min(value = 1, message = "参会人数至少为 1")
        @Max(value = 500, message = "参会人数不能超过 500")
        Integer attendeeCount,

        @NotNull(message = "请选择开始时间")
        @Future(message = "开始时间必须晚于当前时间")
        LocalDateTime startTime,

        @NotNull(message = "请选择结束时间")
        LocalDateTime endTime
) {
}

