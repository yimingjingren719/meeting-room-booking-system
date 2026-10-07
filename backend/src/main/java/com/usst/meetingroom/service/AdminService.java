package com.usst.meetingroom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usst.meetingroom.common.BusinessException;
import com.usst.meetingroom.domain.AuditLog;
import com.usst.meetingroom.domain.Booking;
import com.usst.meetingroom.domain.BookingStatus;
import com.usst.meetingroom.domain.Room;
import com.usst.meetingroom.domain.RoomEquipment;
import com.usst.meetingroom.domain.RoomStatus;
import com.usst.meetingroom.domain.RoomUnavailable;
import com.usst.meetingroom.domain.User;
import com.usst.meetingroom.dto.AuditLogView;
import com.usst.meetingroom.dto.UnavailableView;
import com.usst.meetingroom.dto.UnavailableRequest;
import com.usst.meetingroom.dto.RoomUpsertRequest;
import com.usst.meetingroom.mapper.AuditLogMapper;
import com.usst.meetingroom.mapper.BookingMapper;
import com.usst.meetingroom.mapper.EquipmentMapper;
import com.usst.meetingroom.mapper.RoomMapper;
import com.usst.meetingroom.mapper.RoomEquipmentMapper;
import com.usst.meetingroom.mapper.RoomUnavailableMapper;
import com.usst.meetingroom.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final RoomMapper roomMapper;
    private final RoomUnavailableMapper unavailableMapper;
    private final RoomEquipmentMapper roomEquipmentMapper;
    private final EquipmentMapper equipmentMapper;
    private final BookingMapper bookingMapper;
    private final AuditLogMapper auditLogMapper;
    private final UserMapper userMapper;
    private final AuditService auditService;

    public AdminService(
            RoomMapper roomMapper,
            RoomUnavailableMapper unavailableMapper,
            RoomEquipmentMapper roomEquipmentMapper,
            EquipmentMapper equipmentMapper,
            BookingMapper bookingMapper,
            AuditLogMapper auditLogMapper,
            UserMapper userMapper,
            AuditService auditService
    ) {
        this.roomMapper = roomMapper;
        this.unavailableMapper = unavailableMapper;
        this.roomEquipmentMapper = roomEquipmentMapper;
        this.equipmentMapper = equipmentMapper;
        this.bookingMapper = bookingMapper;
        this.auditLogMapper = auditLogMapper;
        this.userMapper = userMapper;
        this.auditService = auditService;
    }

    @Transactional
    public Room createRoom(Long operatorId, RoomUpsertRequest request) {
        validateRoomRequest(request);
        LocalDateTime now = LocalDateTime.now();
        Room room = new Room();
        applyRoomRequest(room, request);
        room.setCreatedAt(now);
        room.setUpdatedAt(now);
        roomMapper.insert(room);
        replaceEquipment(room.getId(), request.equipmentIds());
        auditService.record(
                operatorId,
                "CREATE_ROOM",
                "ROOM",
                room.getId().toString(),
                "SUCCESS",
                "新增会议室：" + room.getName()
        );
        return room;
    }

    @Transactional
    public Room updateRoom(Long operatorId, Long roomId, RoomUpsertRequest request) {
        validateRoomRequest(request);
        Room room = roomMapper.selectByIdForUpdate(roomId);
        if (room == null) {
            throw new BusinessException(404, "会议室不存在");
        }
        validateRoomChangeAgainstBookings(roomId, request);
        applyRoomRequest(room, request);
        room.setUpdatedAt(LocalDateTime.now());
        roomMapper.updateById(room);
        replaceEquipment(roomId, request.equipmentIds());
        auditService.record(
                operatorId,
                "UPDATE_ROOM",
                "ROOM",
                roomId.toString(),
                "SUCCESS",
                "修改会议室：" + room.getName()
        );
        return room;
    }

    @Transactional
    public void disableRoom(Long operatorId, Long roomId) {
        Room room = roomMapper.selectByIdForUpdate(roomId);
        if (room == null) {
            throw new BusinessException(404, "会议室不存在");
        }
        long activeBookings = bookingMapper.selectCount(
                Wrappers.<Booking>lambdaQuery()
                        .eq(Booking::getRoomId, roomId)
                        .in(
                                Booking::getStatus,
                                BookingStatus.PENDING.name(),
                                BookingStatus.APPROVED.name()
                        )
                        .gt(Booking::getEndTime, LocalDateTime.now())
        );
        if (activeBookings > 0) {
            throw new BusinessException("该会议室仍有未结束的有效预约，暂不能删除");
        }
        room.setStatus(RoomStatus.DISABLED.name());
        room.setUpdatedAt(LocalDateTime.now());
        roomMapper.updateById(room);
        auditService.record(
                operatorId,
                "DISABLE_ROOM",
                "ROOM",
                roomId.toString(),
                "SUCCESS",
                "停用会议室：" + room.getName()
        );
    }

    @Transactional
    public Room updateRoomStatus(Long operatorId, Long roomId, String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        try {
            RoomStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("未知的会议室状态");
        }
        Room room = roomMapper.selectByIdForUpdate(roomId);
        if (room == null) {
            throw new BusinessException(404, "会议室不存在");
        }
        if (normalized.equals(room.getStatus())) {
            return room;
        }
        if (!RoomStatus.AVAILABLE.name().equals(normalized)) {
            long activeBookings = bookingMapper.selectCount(
                    Wrappers.<Booking>lambdaQuery()
                            .eq(Booking::getRoomId, roomId)
                            .in(
                                    Booking::getStatus,
                                    BookingStatus.PENDING.name(),
                                    BookingStatus.APPROVED.name()
                            )
                            .gt(Booking::getEndTime, LocalDateTime.now())
            );
            if (activeBookings > 0) {
                throw new BusinessException("该会议室仍有未结束的有效预约，请先处理相关预约");
            }
        }
        room.setStatus(normalized);
        room.setUpdatedAt(LocalDateTime.now());
        roomMapper.updateById(room);
        auditService.record(
                operatorId,
                "UPDATE_ROOM_STATUS",
                "ROOM",
                roomId.toString(),
                "SUCCESS",
                "会议室状态更新为：" + normalized
        );
        return room;
    }

    @Transactional
    public RoomUnavailable createUnavailable(Long operatorId, UnavailableRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        if (!request.endTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("维护结束时间必须晚于当前时间");
        }
        if (roomMapper.selectByIdForUpdate(request.roomId()) == null) {
            throw new BusinessException(404, "会议室不存在");
        }
        if (bookingMapper.countConflicts(
                request.roomId(),
                request.startTime(),
                request.endTime()
        ) > 0) {
            throw new BusinessException("该时段已有有效预约，暂不能设置为维护时段");
        }
        if (unavailableMapper.countOverlapping(
                request.roomId(),
                request.startTime(),
                request.endTime()
        ) > 0) {
            throw new BusinessException("该会议室在所选时段已存在维护记录");
        }
        RoomUnavailable unavailable = new RoomUnavailable();
        unavailable.setRoomId(request.roomId());
        unavailable.setStartTime(request.startTime());
        unavailable.setEndTime(request.endTime());
        unavailable.setReason(request.reason().trim());
        unavailable.setCreatedBy(operatorId);
        unavailable.setCreatedAt(LocalDateTime.now());
        unavailableMapper.insert(unavailable);
        auditService.record(
                operatorId,
                "CREATE_UNAVAILABLE",
                "ROOM",
                unavailable.getRoomId().toString(),
                "SUCCESS",
                request.reason().trim()
        );
        return unavailable;
    }

    public List<UnavailableView> listUnavailable() {
        List<RoomUnavailable> records = unavailableMapper.selectList(
                Wrappers.<RoomUnavailable>lambdaQuery()
                        .orderByDesc(RoomUnavailable::getStartTime)
                        .last("LIMIT 100")
        );
        Set<Long> roomIds = records.stream()
                .map(RoomUnavailable::getRoomId)
                .collect(Collectors.toSet());
        Map<Long, Room> roomMap = roomIds.isEmpty()
                ? Collections.emptyMap()
                : roomMapper.selectBatchIds(roomIds).stream()
                        .collect(Collectors.toMap(Room::getId, Function.identity()));
        return records.stream().map(record -> {
            Room room = roomMap.get(record.getRoomId());
            return new UnavailableView(
                    record.getId(),
                    record.getRoomId(),
                    room == null ? "未知会议室" : room.getName(),
                    record.getStartTime(),
                    record.getEndTime(),
                    record.getReason(),
                    record.getCreatedAt()
            );
        }).toList();
    }

    public List<AuditLogView> listAuditLogs() {
        List<AuditLog> logs = auditLogMapper.selectList(
                Wrappers.<AuditLog>lambdaQuery()
                        .orderByDesc(AuditLog::getCreatedAt)
                        .last("LIMIT 100")
        );
        Set<Long> operatorIds = logs.stream()
                .map(AuditLog::getOperatorId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = operatorIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(operatorIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));
        return logs.stream().map(log -> {
            User user = userMap.get(log.getOperatorId());
            return new AuditLogView(
                    log.getId(),
                    user == null ? "系统" : user.getDisplayName(),
                    log.getAction(),
                    log.getTargetType(),
                    log.getTargetId(),
                    log.getResult(),
                    log.getDetail(),
                    log.getCreatedAt()
            );
        }).toList();
    }

    private void validateRoomRequest(RoomUpsertRequest request) {
        if (roomMapper.selectById(request.buildingId()) == null) {
            throw new BusinessException(404, "楼栋不存在");
        }
        if (!request.openTime().isBefore(request.closeTime())) {
            throw new BusinessException("开放时间必须早于关闭时间");
        }
        try {
            RoomStatus.valueOf(request.status().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("未知的会议室状态");
        }
    }

    private void validateRoomChangeAgainstBookings(Long roomId, RoomUpsertRequest request) {
        List<Booking> activeBookings = bookingMapper.selectList(
                Wrappers.<Booking>lambdaQuery()
                        .eq(Booking::getRoomId, roomId)
                        .in(
                                Booking::getStatus,
                                BookingStatus.PENDING.name(),
                                BookingStatus.APPROVED.name()
                        )
                        .gt(Booking::getEndTime, LocalDateTime.now())
        );
        if (activeBookings.isEmpty()) {
            return;
        }
        if (!RoomStatus.AVAILABLE.name().equals(
                request.status().trim().toUpperCase(Locale.ROOT)
        )) {
            throw new BusinessException("该会议室仍有未结束的有效预约，不能设置为非开放状态");
        }
        boolean capacityConflict = activeBookings.stream()
                .anyMatch(booking -> booking.getAttendeeCount() > request.capacity());
        if (capacityConflict) {
            throw new BusinessException("新容量小于已有预约的参会人数，请先处理相关预约");
        }
        boolean timeConflict = activeBookings.stream().anyMatch(booking ->
                booking.getStartTime().toLocalTime().isBefore(request.openTime())
                        || booking.getEndTime().toLocalTime().isAfter(request.closeTime())
        );
        if (timeConflict) {
            throw new BusinessException("新开放时间与已有预约不兼容，请先调整或取消相关预约");
        }
    }

    private void applyRoomRequest(Room room, RoomUpsertRequest request) {
        room.setBuildingId(request.buildingId());
        room.setName(request.name().trim());
        room.setFloorNo(request.floorNo());
        room.setLocation(request.location().trim());
        room.setCapacity(request.capacity());
        room.setOpenTime(request.openTime());
        room.setCloseTime(request.closeTime());
        room.setStatus(request.status().trim().toUpperCase(Locale.ROOT));
        room.setImageUrl(request.imageUrl());
        room.setDescription(request.description());
    }

    private void replaceEquipment(Long roomId, List<Long> equipmentIds) {
        roomEquipmentMapper.delete(
                Wrappers.<RoomEquipment>lambdaQuery().eq(RoomEquipment::getRoomId, roomId)
        );
        if (equipmentIds == null || equipmentIds.isEmpty()) {
            return;
        }
        for (Long equipmentId : equipmentIds.stream().distinct().toList()) {
            if (equipmentMapper.selectById(equipmentId) == null) {
                throw new BusinessException("设备不存在：ID " + equipmentId);
            }
            RoomEquipment link = new RoomEquipment();
            link.setRoomId(roomId);
            link.setEquipmentId(equipmentId);
            link.setQuantity(1);
            roomEquipmentMapper.insert(link);
        }
    }

}

