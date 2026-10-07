package com.usst.meetingroom.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    private final BookingService bookingService;

    public ScheduledTasks(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void closeFinishedBookings() {
        bookingService.refreshExpiredStatuses();
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void sendBookingReminders() {
        bookingService.sendUpcomingReminders();
    }
}
