package com.usst.meetingroom.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApprovalRequest(
        @NotNull(message = "请选择审批结果")
        Boolean approved,

        @Size(max = 500, message = "审批意见不能超过 500 个字符")
        String comment
) {
}

