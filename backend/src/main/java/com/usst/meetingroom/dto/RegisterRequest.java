package com.usst.meetingroom.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "请输入用户名")
        @Pattern(regexp = "^[A-Za-z0-9_]{4,30}$", message = "用户名需为 4-30 位字母、数字或下划线")
        String username,

        @NotBlank(message = "请输入姓名")
        @Size(max = 50, message = "姓名不能超过 50 个字符")
        String displayName,

        @NotBlank(message = "请输入密码")
        @Size(min = 6, max = 50, message = "密码长度需为 6-50 位")
        String password,

        @Email(message = "邮箱格式不正确")
        String email,

        String phone
) {
}

