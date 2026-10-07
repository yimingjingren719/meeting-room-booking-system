package com.usst.meetingroom.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.List;

public record RoomUpsertRequest(
        @NotNull(message = "请选择楼栋")
        Long buildingId,

        @NotBlank(message = "请输入会议室名称")
        @Size(max = 80, message = "会议室名称不能超过 80 个字符")
        String name,

        @NotNull(message = "请输入楼层")
        @Min(value = -5, message = "楼层不能小于 -5")
        @Max(value = 120, message = "楼层不能大于 120")
        Integer floorNo,

        @NotBlank(message = "请输入具体位置")
        String location,

        @NotNull(message = "请输入容量")
        @Min(value = 1, message = "容量至少为 1")
        Integer capacity,

        @NotNull(message = "请选择开放时间")
        LocalTime openTime,

        @NotNull(message = "请选择关闭时间")
        LocalTime closeTime,

        @NotBlank(message = "请选择会议室状态")
        String status,

        String imageUrl,

        @Size(max = 500, message = "描述不能超过 500 个字符")
        String description,

        List<Long> equipmentIds
) {
}
