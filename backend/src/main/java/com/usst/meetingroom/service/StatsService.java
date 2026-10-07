package com.usst.meetingroom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usst.meetingroom.common.StatusText;
import com.usst.meetingroom.domain.Booking;
import com.usst.meetingroom.domain.BookingStatus;
import com.usst.meetingroom.domain.Room;
import com.usst.meetingroom.dto.StatsResponse;
import com.usst.meetingroom.mapper.BookingMapper;
import com.usst.meetingroom.mapper.RoomMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private final BookingMapper bookingMapper;
    private final RoomMapper roomMapper;
    private final BookingService bookingService;

    public StatsService(
            BookingMapper bookingMapper,
            RoomMapper roomMapper,
            BookingService bookingService
    ) {
        this.bookingMapper = bookingMapper;
        this.roomMapper = roomMapper;
        this.bookingService = bookingService;
    }

    public StatsResponse overview() {
        bookingService.refreshExpiredStatuses();
        List<Booking> bookings = bookingMapper.selectList(
                Wrappers.<Booking>lambdaQuery().orderByAsc(Booking::getStartTime)
        );
        List<Room> rooms = roomMapper.selectList(Wrappers.<Room>lambdaQuery());
        LocalDate today = LocalDate.now();

        long total = bookings.size();
        long pending = countStatus(bookings, BookingStatus.PENDING);
        long approved = countStatus(bookings, BookingStatus.APPROVED)
                + countStatus(bookings, BookingStatus.COMPLETED);
        long rejected = countStatus(bookings, BookingStatus.REJECTED);
        long todayCount = bookings.stream()
                .filter(booking -> booking.getStartTime().toLocalDate().equals(today))
                .filter(booking -> List.of(
                        BookingStatus.PENDING.name(),
                        BookingStatus.APPROVED.name(),
                        BookingStatus.COMPLETED.name()
                ).contains(booking.getStatus()))
                .count();
        long processed = approved + rejected;
        double approvalRate = processed == 0 ? 0 : Math.round(approved * 1000.0 / processed) / 10.0;

        Map<Long, String> roomNames = rooms.stream()
                .collect(Collectors.toMap(Room::getId, Room::getName));
        Map<Long, Long> roomUsage = bookings.stream()
                .filter(booking -> List.of(
                        BookingStatus.APPROVED.name(),
                        BookingStatus.COMPLETED.name()
                ).contains(booking.getStatus()))
                .collect(Collectors.groupingBy(Booking::getRoomId, Collectors.counting()));
        List<StatsResponse.ChartItem> usageItems = roomUsage.entrySet().stream()
                .map(entry -> new StatsResponse.ChartItem(
                        roomNames.getOrDefault(entry.getKey(), "未知会议室"),
                        entry.getValue()
                ))
                .sorted(Comparator.comparingLong(StatsResponse.ChartItem::value).reversed())
                .limit(6)
                .toList();

        Map<String, Long> statusCounts = bookings.stream()
                .collect(Collectors.groupingBy(Booking::getStatus, Collectors.counting()));
        List<StatsResponse.ChartItem> statusItems = List.of(
                BookingStatus.PENDING,
                BookingStatus.APPROVED,
                BookingStatus.REJECTED,
                BookingStatus.CANCELLED,
                BookingStatus.COMPLETED,
                BookingStatus.EXPIRED
        ).stream()
                .map(status -> new StatsResponse.ChartItem(
                        StatusText.booking(status.name()),
                        statusCounts.getOrDefault(status.name(), 0L)
                ))
                .toList();

        Map<LocalDate, Long> trendCounts = bookings.stream()
                .filter(booking -> !booking.getStartTime().toLocalDate().isBefore(today.minusDays(6)))
                .filter(booking -> !booking.getStartTime().toLocalDate().isAfter(today))
                .collect(Collectors.groupingBy(
                        booking -> booking.getStartTime().toLocalDate(),
                        Collectors.counting()
                ));
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MM-dd");
        List<StatsResponse.ChartItem> trend = new ArrayList<>();
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            trend.add(new StatsResponse.ChartItem(
                    date.format(labelFormatter),
                    trendCounts.getOrDefault(date, 0L)
            ));
        }

        return new StatsResponse(
                total,
                pending,
                approved,
                todayCount,
                approvalRate,
                usageItems,
                statusItems,
                trend
        );
    }

    private long countStatus(List<Booking> bookings, BookingStatus status) {
        return bookings.stream()
                .filter(booking -> status.name().equals(booking.getStatus()))
                .count();
    }
}

