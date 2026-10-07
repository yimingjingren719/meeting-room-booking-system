package com.usst.meetingroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "请输入当前密码")
        String currentPassword,

        @NotBlank(message = "请输入新密码")
        @Size(min = 6, max = 50, message = "新密码长度需为 6-50 位")
        String newPassword
) {
}

