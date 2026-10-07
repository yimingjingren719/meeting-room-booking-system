package com.usst.meetingroom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usst.meetingroom.common.BusinessException;
import com.usst.meetingroom.common.StatusText;
import com.usst.meetingroom.domain.Booking;
import com.usst.meetingroom.domain.BookingApproval;
import com.usst.meetingroom.domain.BookingStatus;
import com.usst.meetingroom.domain.Building;
import com.usst.meetingroom.domain.Notification;
import com.usst.meetingroom.domain.Room;
import com.usst.meetingroom.domain.User;
import com.usst.meetingroom.dto.ApprovalRequest;
import com.usst.meetingroom.dto.BookingCreateRequest;
import com.usst.meetingroom.dto.BookingView;
import com.usst.meetingroom.dto.CancelBookingRequest;
import com.usst.meetingroom.mapper.BookingApprovalMapper;
import com.usst.meetingroom.mapper.BookingMapper;
import com.usst.meetingroom.mapper.BuildingMapper;
import com.usst.meetingroom.mapper.NotificationMapper;
import com.usst.meetingroom.mapper.RoleMapper;
import com.usst.meetingroom.mapper.RoomMapper;
import com.usst.meetingroom.mapper.RoomUnavailableMapper;
import com.usst.meetingroom.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingMapper bookingMapper;
    private final BookingApprovalMapper approvalMapper;
    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;
    private final UserMapper userMapper;
    private final NotificationMapper notificationMapper;
    private final RoomUnavailableMapper unavailableMapper;
    private final RoleMapper roleMapper;
    private final AuditService auditService;
    private final UserService userService;

    public BookingService(
            BookingMapper bookingMapper,
            BookingApprovalMapper approvalMapper,
            RoomMapper roomMapper,
            BuildingMapper buildingMapper,
            UserMapper userMapper,
            NotificationMapper notificationMapper,
            RoomUnavailableMapper unavailableMapper,
            RoleMapper roleMapper,
            AuditService auditService,
            UserService userService
    ) {
        this.bookingMapper = bookingMapper;
        this.approvalMapper = approvalMapper;
        this.roomMapper = roomMapper;
        this.buildingMapper = buildingMapper;
        this.userMapper = userMapper;
        this.notificationMapper = notificationMapper;
        this.unavailableMapper = unavailableMapper;
        this.roleMapper = roleMapper;
        this.auditService = auditService;
        this.userService = userService;
    }

    @Transactional
    public BookingView create(Long userId, BookingCreateRequest request) {
        userService.requireBookingAllowed(userId);
        validateTime(request.startTime(), request.endTime());
        Room room = roomMapper.selectByIdForUpdate(request.roomId());
        if (room == null) {
            throw new BusinessException(404, "会议室不存在");
        }
        validateRoom(room, request);
        assertNoConflict(room.getId(), null, request.startTime(), request.endTime());

        LocalDateTime now = LocalDateTime.now();
        Booking booking = new Booking();
        booking.setBookingNo(generateBookingNo());
        booking.setUserId(userId);
        booking.setRoomId(room.getId());
        booking.setSubject(request.subject().trim());
        booking.setPurpose(request.purpose());
        booking.setAttendeeCount(request.attendeeCount());
        booking.setStartTime(request.startTime());
        booking.setEndTime(request.endTime());
        booking.setStatus(BookingStatus.PENDING.name());
        booking.setReminderSent(false);
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);
        bookingMapper.insert(booking);

        for (Long approverId : roleMapper.selectApproverIds()) {
            createNotification(
                    approverId,
                    booking.getId(),
                    "有新的预约待审批",
                    room.getName() + "的预约“" + booking.getSubject() + "”等待处理。"
            );
        }
        auditService.record(userId, "CREATE_BOOKING", "BOOKING", booking.getId().toString(),
                "SUCCESS", "提交预约：" + booking.getBookingNo());
        return toViews(List.of(booking)).get(0);
    }

    public List<BookingView> listMine(Long userId, String status) {
        var query = Wrappers.<Booking>lambdaQuery()
                .eq(Booking::getUserId, userId)
                .orderByDesc(Booking::getStartTime);
        if (StringUtils.hasText(status)) {
            query.eq(Booking::getStatus, normalizeStatus(status));
        }
        return toViews(bookingMapper.selectList(query));
    }

    public List<BookingView> listAll(String status) {
        var query = Wrappers.<Booking>lambdaQuery().orderByDesc(Booking::getCreatedAt);
        if (StringUtils.hasText(status)) {
            query.eq(Booking::getStatus, normalizeStatus(status));
        }
        return toViews(bookingMapper.selectList(query));
    }

    @Transactional
    public BookingView approve(Long approverId, Long bookingId, ApprovalRequest request) {
        Booking booking = bookingMapper.selectByIdForUpdate(bookingId);
        if (booking == null) {
            throw new BusinessException(404, "预约记录不存在");
        }
        if (!BookingStatus.PENDING.name().equals(booking.getStatus())) {
            throw new BusinessException("该预约已处理，不能重复审批");
        }

        if (Boolean.TRUE.equals(request.approved())) {
            roomMapper.selectByIdForUpdate(booking.getRoomId());
            assertNoConflict(
                    booking.getRoomId(),
                    booking.getId(),
                    booking.getStartTime(),
                    booking.getEndTime()
            );
            booking.setStatus(BookingStatus.APPROVED.name());
        } else {
            if (!StringUtils.hasText(request.comment())) {
                throw new BusinessException("驳回预约时必须填写审批意见");
            }
            booking.setStatus(BookingStatus.REJECTED.name());
        }
        booking.setUpdatedAt(LocalDateTime.now());
        bookingMapper.updateById(booking);

        BookingApproval approval = new BookingApproval();
        approval.setBookingId(booking.getId());
        approval.setApproverId(approverId);
        approval.setResult(Boolean.TRUE.equals(request.approved()) ? "APPROVED" : "REJECTED");
        approval.setComment(request.comment());
        approval.setCreatedAt(LocalDateTime.now());
        approvalMapper.insert(approval);

        String resultText = Boolean.TRUE.equals(request.approved()) ? "已通过" : "已驳回";
        createNotification(
                booking.getUserId(),
                booking.getId(),
                "预约审批结果",
                "预约“" + booking.getSubject() + "”" + resultText
                        + (StringUtils.hasText(request.comment()) ? "，审批意见：" + request.comment() : "。")
        );
        auditService.record(
                approverId,
                Boolean.TRUE.equals(request.approved()) ? "APPROVE_BOOKING" : "REJECT_BOOKING",
                "BOOKING",
                booking.getId().toString(),
                "SUCCESS",
                request.comment()
        );
        return toViews(List.of(booking)).get(0);
    }

    @Transactional
    public BookingView cancel(Long userId, boolean canManageAll, Long bookingId, CancelBookingRequest request) {
        Booking booking = bookingMapper.selectByIdForUpdate(bookingId);
        if (booking == null) {
            throw new BusinessException(404, "预约记录不存在");
        }
        if (!canManageAll && !booking.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能取消本人预约");
        }
        if (!BookingStatus.PENDING.name().equals(booking.getStatus())
                && !BookingStatus.APPROVED.name().equals(booking.getStatus())) {
            throw new BusinessException("当前预约状态不支持取消");
        }
        if (!booking.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("预约已经开始，不能取消");
        }

        booking.setStatus(BookingStatus.CANCELLED.name());
        booking.setCancelReason(
                StringUtils.hasText(request.reason()) ? request.reason().trim() : "用户主动取消"
        );
        booking.setUpdatedAt(LocalDateTime.now());
        bookingMapper.updateById(booking);
        String cancelReason = booking.getCancelReason();
        for (Long approverId : roleMapper.selectApproverIds()) {
            createNotification(
                    approverId,
                    booking.getId(),
                    "预约已取消",
                    "预约“" + booking.getSubject() + "”已被取消，原因：" + cancelReason
            );
        }
        if (canManageAll && !booking.getUserId().equals(userId)) {
            createNotification(
                    booking.getUserId(),
                    booking.getId(),
                    "预约已被管理员取消",
                    "预约“" + booking.getSubject() + "”已被管理员取消，原因：" + cancelReason
            );
        }
        auditService.record(
                userId,
                "CANCEL_BOOKING",
                "BOOKING",
                booking.getId().toString(),
                "SUCCESS",
                booking.getCancelReason()
        );
        return toViews(List.of(booking)).get(0);
    }

    @Transactional
    public void refreshExpiredStatuses() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expired = bookingMapper.selectList(
                Wrappers.<Booking>lambdaQuery()
                        .in(Booking::getStatus,
                                BookingStatus.PENDING.name(),
                                BookingStatus.APPROVED.name())
                        .lt(Booking::getEndTime, now)
        );
        for (Booking booking : expired) {
            booking.setStatus(
                    BookingStatus.PENDING.name().equals(booking.getStatus())
                            ? BookingStatus.EXPIRED.name()
                            : BookingStatus.COMPLETED.name()
            );
            booking.setUpdatedAt(now);
            bookingMapper.updateById(booking);
        }
    }

    @Transactional
    public int sendUpcomingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderDeadline = now.plusMinutes(30);
        List<Booking> upcoming = bookingMapper.selectList(
                Wrappers.<Booking>lambdaQuery()
                        .eq(Booking::getStatus, BookingStatus.APPROVED.name())
                        .eq(Booking::getReminderSent, false)
                        .gt(Booking::getStartTime, now)
                        .le(Booking::getStartTime, reminderDeadline)
        );
        for (Booking booking : upcoming) {
            createNotification(
                    booking.getUserId(),
                    booking.getId(),
                    "会议即将开始",
                    "预约“" + booking.getSubject() + "”将于 "
                            + booking.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                            + " 开始，请提前到场。"
            );
            booking.setReminderSent(true);
            booking.setUpdatedAt(now);
            bookingMapper.updateById(booking);
        }
        return upcoming.size();
    }

    private void validateTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
            throw new BusinessException("当前版本仅支持当天预约");
        }
        if (!startTime.isAfter(LocalDateTime.now())) {
            throw new BusinessException("开始时间必须晚于当前时间");
        }
    }

    private void validateRoom(Room room, BookingCreateRequest request) {
        if (!"AVAILABLE".equals(room.getStatus())) {
            throw new BusinessException("该会议室当前暂停开放");
        }
        if (request.attendeeCount() > room.getCapacity()) {
            throw new BusinessException("参会人数超过会议室容量");
        }
        LocalDate date = request.startTime().toLocalDate();
        LocalDateTime open = LocalDateTime.of(date, room.getOpenTime());
        LocalDateTime close = LocalDateTime.of(date, room.getCloseTime());
        if (request.startTime().isBefore(open) || request.endTime().isAfter(close)) {
            throw new BusinessException("预约时间超出会议室开放时段");
        }
    }

    private void assertNoConflict(
            Long roomId,
            Long excludedBookingId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        long conflicts = excludedBookingId == null
                ? bookingMapper.countConflicts(roomId, startTime, endTime)
                : bookingMapper.countConflictsExcluding(
                        roomId, excludedBookingId, startTime, endTime
                );
        if (conflicts > 0) {
            throw new BusinessException("该时段已被其他预约占用，请重新选择");
        }
        if (unavailableMapper.countOverlapping(roomId, startTime, endTime) > 0) {
            throw new BusinessException("该时段会议室维护中，暂不可预约");
        }
    }

    private String generateBookingNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 4)
                .toUpperCase(Locale.ROOT);
        return "MR" + timestamp + suffix;
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        try {
            BookingStatus.valueOf(normalized);
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("未知的预约状态");
        }
    }

    private List<BookingView> toViews(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = bookings.stream().map(Booking::getUserId).collect(Collectors.toSet());
        Set<Long> roomIds = bookings.stream().map(Booking::getRoomId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<Room> rooms = roomMapper.selectBatchIds(roomIds);
        Map<Long, Room> roomMap = rooms.stream()
                .collect(Collectors.toMap(Room::getId, Function.identity()));
        Set<Long> buildingIds = rooms.stream().map(Room::getBuildingId).collect(Collectors.toSet());
        Map<Long, Building> buildingMap = buildingMapper.selectBatchIds(buildingIds).stream()
                .collect(Collectors.toMap(Building::getId, Function.identity()));

        Set<Long> bookingIds = bookings.stream().map(Booking::getId).collect(Collectors.toSet());
        List<BookingApproval> approvals = approvalMapper.selectList(
                Wrappers.<BookingApproval>lambdaQuery().in(BookingApproval::getBookingId, bookingIds)
        );
        Map<Long, BookingApproval> approvalMap = approvals.stream()
                .collect(Collectors.toMap(BookingApproval::getBookingId, Function.identity()));
        Set<Long> approverIds = approvals.stream()
                .map(BookingApproval::getApproverId)
                .collect(Collectors.toSet());
        Map<Long, User> approverMap = approverIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(approverIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        return bookings.stream().map(booking -> {
            Room room = roomMap.get(booking.getRoomId());
            User applicant = userMap.get(booking.getUserId());
            Building building = room == null ? null : buildingMap.get(room.getBuildingId());
            BookingApproval approval = approvalMap.get(booking.getId());
            User approver = approval == null ? null : approverMap.get(approval.getApproverId());
            return new BookingView(
                    booking.getId(),
                    booking.getBookingNo(),
                    booking.getUserId(),
                    applicant == null ? "" : applicant.getDisplayName(),
                    booking.getRoomId(),
                    room == null ? "" : room.getName(),
                    building == null ? "" : building.getName(),
                    room == null ? "" : room.getLocation(),
                    room == null ? 0 : room.getCapacity(),
                    booking.getSubject(),
                    booking.getPurpose(),
                    booking.getAttendeeCount(),
                    booking.getStartTime(),
                    booking.getEndTime(),
                    booking.getStatus(),
                    StatusText.booking(booking.getStatus()),
                    booking.getCancelReason(),
                    approval == null ? null : approval.getComment(),
                    approver == null ? null : approver.getDisplayName(),
                    booking.getCreatedAt()
            );
        }).sorted(Comparator.comparing(BookingView::startTime).reversed()).toList();
    }

    private void createNotification(
            Long recipientId,
            Long bookingId,
            String title,
            String content
    ) {
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setBookingId(bookingId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReadFlag(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
    }

}
