package com.usst.meetingroom.common;

import java.util.Map;

public final class StatusText {

    private static final Map<String, String> BOOKING_STATUS = Map.of(
            "PENDING", "待审批",
            "APPROVED", "已通过",
            "REJECTED", "已驳回",
            "CANCELLED", "已取消",
            "COMPLETED", "已完成",
            "EXPIRED", "已过期"
    );

    private StatusText() {
    }

    public static String booking(String status) {
        return BOOKING_STATUS.getOrDefault(status, status);
    }
}

