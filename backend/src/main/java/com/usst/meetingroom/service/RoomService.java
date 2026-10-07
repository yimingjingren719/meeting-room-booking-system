package com.usst.meetingroom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usst.meetingroom.common.BusinessException;
import com.usst.meetingroom.domain.Building;
import com.usst.meetingroom.domain.Equipment;
import com.usst.meetingroom.domain.Room;
import com.usst.meetingroom.domain.RoomEquipment;
import com.usst.meetingroom.domain.UserFavoriteRoom;
import com.usst.meetingroom.dto.AvailabilitySlot;
import com.usst.meetingroom.dto.RoomResponse;
import com.usst.meetingroom.mapper.BookingMapper;
import com.usst.meetingroom.mapper.BuildingMapper;
import com.usst.meetingroom.mapper.EquipmentMapper;
import com.usst.meetingroom.mapper.RoomEquipmentMapper;
import com.usst.meetingroom.mapper.RoomMapper;
import com.usst.meetingroom.mapper.RoomUnavailableMapper;
import com.usst.meetingroom.mapper.UserFavoriteRoomMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;
    private final EquipmentMapper equipmentMapper;
    private final RoomEquipmentMapper roomEquipmentMapper;
    private final BookingMapper bookingMapper;
    private final RoomUnavailableMapper unavailableMapper;
    private final UserFavoriteRoomMapper favoriteRoomMapper;
    private final AuditService auditService;

    public RoomService(
            RoomMapper roomMapper,
            BuildingMapper buildingMapper,
            EquipmentMapper equipmentMapper,
            RoomEquipmentMapper roomEquipmentMapper,
            BookingMapper bookingMapper,
            RoomUnavailableMapper unavailableMapper,
            UserFavoriteRoomMapper favoriteRoomMapper,
            AuditService auditService
    ) {
        this.roomMapper = roomMapper;
        this.buildingMapper = buildingMapper;
        this.equipmentMapper = equipmentMapper;
        this.roomEquipmentMapper = roomEquipmentMapper;
        this.bookingMapper = bookingMapper;
        this.unavailableMapper = unavailableMapper;
        this.favoriteRoomMapper = favoriteRoomMapper;
        this.auditService = auditService;
    }

    public List<RoomResponse> listRooms(
            Long buildingId,
            Integer capacity,
            String equipment,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            boolean includeDisabled
    ) {
        return listRooms(
                null,
                buildingId,
                capacity,
                equipment,
                date,
                startTime,
                endTime,
                includeDisabled
        );
    }

    public List<RoomResponse> listRooms(
            Long userId,
            Long buildingId,
            Integer capacity,
            String equipment,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            boolean includeDisabled
    ) {
        var query = Wrappers.<Room>lambdaQuery().orderByAsc(Room::getBuildingId, Room::getFloorNo);
        if (!includeDisabled) {
            query.ne(Room::getStatus, "DISABLED");
        }
        if (buildingId != null) {
            query.eq(Room::getBuildingId, buildingId);
        }
        if (capacity != null) {
            query.ge(Room::getCapacity, capacity);
        }

        List<Room> rooms = roomMapper.selectList(query);
        if (StringUtils.hasText(equipment)) {
            rooms = filterByEquipment(rooms, equipment.trim());
        }
        if (date != null && startTime != null && endTime != null) {
            LocalDateTime start = LocalDateTime.of(date, startTime);
            LocalDateTime end = LocalDateTime.of(date, endTime);
            rooms = rooms.stream()
                    .filter(room -> "AVAILABLE".equals(room.getStatus()))
                    .filter(room -> startTime.isBefore(endTime))
                    .filter(room -> !startTime.isBefore(room.getOpenTime())
                            && !endTime.isAfter(room.getCloseTime()))
                    .filter(room -> bookingMapper.countConflicts(room.getId(), start, end) == 0)
                    .filter(room -> unavailableMapper.countOverlapping(room.getId(), start, end) == 0)
                    .toList();
        }
        return toResponses(rooms, date, userId);
    }

    public RoomResponse getRoom(Long id, LocalDate date, Long userId) {
        Room room = requireRoom(id);
        return toResponses(List.of(room), date, userId).get(0);
    }

    public Room requireRoom(Long id) {
        Room room = roomMapper.selectById(id);
        if (room == null) {
            throw new BusinessException(404, "会议室不存在");
        }
        return room;
    }

    public List<Building> listBuildings() {
        return buildingMapper.selectList(
                Wrappers.<Building>lambdaQuery()
                        .eq(Building::getStatus, "ACTIVE")
                        .orderByAsc(Building::getId)
        );
    }

    public List<Equipment> listEquipment() {
        return equipmentMapper.selectList(
                Wrappers.<Equipment>lambdaQuery().orderByAsc(Equipment::getId)
        );
    }

    public List<AvailabilitySlot> availabilityOf(Long roomId, LocalDate date) {
        Room room = requireRoom(roomId);
        return buildAvailability(room, date);
    }

    private List<Room> filterByEquipment(List<Room> rooms, String equipmentName) {
        List<Equipment> matches = equipmentMapper.selectList(
                Wrappers.<Equipment>lambdaQuery().like(Equipment::getName, equipmentName)
        );
        if (matches.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> equipmentIds = matches.stream().map(Equipment::getId).collect(Collectors.toSet());
        List<Long> roomIds = roomEquipmentMapper.selectList(
                Wrappers.<RoomEquipment>lambdaQuery().in(RoomEquipment::getEquipmentId, equipmentIds)
        ).stream().map(RoomEquipment::getRoomId).distinct().toList();
        return rooms.stream().filter(room -> roomIds.contains(room.getId())).toList();
    }

    private List<RoomResponse> toResponses(List<Room> rooms, LocalDate date, Long userId) {
        if (rooms.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> buildingIds = rooms.stream().map(Room::getBuildingId).collect(Collectors.toSet());
        Map<Long, Building> buildingMap = buildingMapper.selectBatchIds(buildingIds).stream()
                .collect(Collectors.toMap(Building::getId, Function.identity()));
        Set<Long> favoriteRoomIds = userId == null
                ? Collections.emptySet()
                : favoriteRoomMapper.selectList(
                                Wrappers.<UserFavoriteRoom>lambdaQuery()
                                        .eq(UserFavoriteRoom::getUserId, userId)
                                        .in(
                                                UserFavoriteRoom::getRoomId,
                                                rooms.stream().map(Room::getId).toList()
                                        )
                        ).stream()
                        .map(UserFavoriteRoom::getRoomId)
                        .collect(Collectors.toSet());

        return rooms.stream().map(room -> {
            Building building = buildingMap.get(room.getBuildingId());
            List<String> equipmentNames = equipmentMapper.selectByRoomId(room.getId()).stream()
                    .map(Equipment::getName)
                    .toList();
            return new RoomResponse(
                    room.getId(),
                    room.getName(),
                    room.getBuildingId(),
                    building == null ? "" : building.getName(),
                    building == null ? "" : building.getCampus(),
                    room.getFloorNo(),
                    room.getLocation(),
                    room.getCapacity(),
                    room.getOpenTime(),
                    room.getCloseTime(),
                    room.getStatus(),
                    room.getImageUrl(),
                    room.getDescription(),
                    equipmentNames,
                    date == null ? List.of() : buildAvailability(room, date),
                    favoriteRoomIds.contains(room.getId()),
                    null
            );
        }).toList();
    }

    @Transactional
    public void addFavorite(Long userId, Long roomId) {
        requireRoom(roomId);
        Long count = favoriteRoomMapper.selectCount(
                Wrappers.<UserFavoriteRoom>lambdaQuery()
                        .eq(UserFavoriteRoom::getUserId, userId)
                        .eq(UserFavoriteRoom::getRoomId, roomId)
        );
        if (count == 0) {
            UserFavoriteRoom favorite = new UserFavoriteRoom();
            favorite.setUserId(userId);
            favorite.setRoomId(roomId);
            favorite.setCreatedAt(LocalDateTime.now());
            favoriteRoomMapper.insert(favorite);
            auditService.record(
                    userId,
                    "ADD_FAVORITE_ROOM",
                    "ROOM",
                    roomId.toString(),
                    "SUCCESS",
                    "收藏常用会议室"
            );
        }
    }

    @Transactional
    public void removeFavorite(Long userId, Long roomId) {
        favoriteRoomMapper.delete(
                Wrappers.<UserFavoriteRoom>lambdaQuery()
                        .eq(UserFavoriteRoom::getUserId, userId)
                        .eq(UserFavoriteRoom::getRoomId, roomId)
        );
        auditService.record(
                userId,
                "REMOVE_FAVORITE_ROOM",
                "ROOM",
                roomId.toString(),
                "SUCCESS",
                "取消收藏会议室"
        );
    }

    public List<RoomResponse> recommend(
            Long userId,
            Integer attendeeCount,
            String equipment,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {
        List<RoomResponse> rooms = listRooms(
                userId,
                null,
                attendeeCount,
                equipment,
                date,
                startTime,
                endTime,
                false
        );
        return rooms.stream()
                .map(room -> {
                    int excess = Math.max(0, room.capacity() - attendeeCount);
                    int score = Math.max(40, 100 - Math.min(55, excess * 4));
                    score += Boolean.TRUE.equals(room.favorite()) ? 5 : 0;
                    return withRecommendationScore(room, Math.min(100, score));
                })
                .sorted(Comparator.comparingInt(RoomResponse::recommendationScore).reversed())
                .limit(6)
                .toList();
    }

    private RoomResponse withRecommendationScore(RoomResponse room, int score) {
        return new RoomResponse(
                room.id(),
                room.name(),
                room.buildingId(),
                room.buildingName(),
                room.campus(),
                room.floorNo(),
                room.location(),
                room.capacity(),
                room.openTime(),
                room.closeTime(),
                room.status(),
                room.imageUrl(),
                room.description(),
                room.equipment(),
                room.availability(),
                room.favorite(),
                score
        );
    }

    private List<AvailabilitySlot> buildAvailability(Room room, LocalDate date) {
        List<AvailabilitySlot> slots = new ArrayList<>();
        LocalTime cursor = truncateToHour(room.getOpenTime());
        LocalTime closeTime = room.getCloseTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();

        while (cursor.isBefore(closeTime)) {
            LocalTime next = cursor.plusHours(1);
            if (next.isAfter(closeTime)) {
                next = closeTime;
            }
            LocalDateTime start = LocalDateTime.of(date, cursor);
            LocalDateTime end = LocalDateTime.of(date, next);
            String status;
            String statusText;

            if (!"AVAILABLE".equals(room.getStatus())) {
                status = "UNAVAILABLE";
                statusText = "暂停开放";
            } else if (!end.isAfter(now)) {
                status = "PAST";
                statusText = "已结束";
            } else if (start.isBefore(now)) {
                status = "UNAVAILABLE";
                statusText = "已开始";
            } else if (unavailableMapper.countOverlapping(room.getId(), start, end) > 0) {
                status = "UNAVAILABLE";
                statusText = "维护占用";
            } else if (bookingMapper.countConflicts(room.getId(), start, end) > 0) {
                status = "OCCUPIED";
                statusText = "已占用";
            } else {
                status = "AVAILABLE";
                statusText = "可预约";
            }
            slots.add(new AvailabilitySlot(
                    cursor,
                    next,
                    status,
                    statusText + " " + cursor.format(formatter) + "-" + next.format(formatter)
            ));
            cursor = next;
        }
        return slots;
    }

    private LocalTime truncateToHour(LocalTime time) {
        return LocalTime.of(time.getHour(), 0);
    }
}

