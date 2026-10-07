package com.usst.meetingroom.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "请输入姓名")
        @Size(max = 50, message = "姓名不能超过 50 个字符")
        String displayName,

        @Email(message = "邮箱格式不正确")
        String email,

        @Size(max = 30, message = "手机号不能超过 30 个字符")
        String phone
) {
}

