package com.usst.meetingroom.dto;

import java.util.List;

public record StatsResponse(
        long totalBookings,
        long pendingBookings,
        long approvedBookings,
        long todayBookings,
        double approvalRate,
        List<ChartItem> roomUsage,
        List<ChartItem> statusDistribution,
        List<ChartItem> bookingTrend
) {
    public record ChartItem(String label, long value) {
    }
}

