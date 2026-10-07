package com.usst.meetingroom.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usst.meetingroom.domain.Booking;
import com.usst.meetingroom.domain.BookingApproval;
import com.usst.meetingroom.domain.BookingStatus;
import com.usst.meetingroom.domain.Building;
import com.usst.meetingroom.domain.Equipment;
import com.usst.meetingroom.domain.Notification;
import com.usst.meetingroom.domain.Role;
import com.usst.meetingroom.domain.Room;
import com.usst.meetingroom.domain.RoomEquipment;
import com.usst.meetingroom.domain.User;
import com.usst.meetingroom.domain.UserRole;
import com.usst.meetingroom.mapper.BookingApprovalMapper;
import com.usst.meetingroom.mapper.BookingMapper;
import com.usst.meetingroom.mapper.BuildingMapper;
import com.usst.meetingroom.mapper.EquipmentMapper;
import com.usst.meetingroom.mapper.NotificationMapper;
import com.usst.meetingroom.mapper.RoleMapper;
import com.usst.meetingroom.mapper.RoomEquipmentMapper;
import com.usst.meetingroom.mapper.RoomMapper;
import com.usst.meetingroom.mapper.UserMapper;
import com.usst.meetingroom.mapper.UserRoleMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
@Order(10)
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final BuildingMapper buildingMapper;
    private final RoomMapper roomMapper;
    private final EquipmentMapper equipmentMapper;
    private final RoomEquipmentMapper roomEquipmentMapper;
    private final BookingMapper bookingMapper;
    private final BookingApprovalMapper approvalMapper;
    private final NotificationMapper notificationMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserMapper userMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            BuildingMapper buildingMapper,
            RoomMapper roomMapper,
            EquipmentMapper equipmentMapper,
            RoomEquipmentMapper roomEquipmentMapper,
            BookingMapper bookingMapper,
            BookingApprovalMapper approvalMapper,
            NotificationMapper notificationMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.buildingMapper = buildingMapper;
        this.roomMapper = roomMapper;
        this.equipmentMapper = equipmentMapper;
        this.roomEquipmentMapper = roomEquipmentMapper;
        this.bookingMapper = bookingMapper;
        this.approvalMapper = approvalMapper;
        this.notificationMapper = notificationMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (roleMapper.selectCount(null) == 0) {
            createRole("USER", "普通用户");
            createRole("APPROVER", "审批管理员");
            createRole("ADMIN", "系统管理员");
        }
        if (userMapper.selectCount(null) == 0) {
            createDemoUsers();
        }
        if (buildingMapper.selectCount(null) == 0) {
            createRooms();
        }
        if (bookingMapper.selectCount(null) == 0) {
            createDemoBookings();
        }
    }

    private void createRole(String code, String name) {
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        roleMapper.insert(role);
    }

    private void createDemoUsers() {
        createUser("user", "user123", "李同学");
        createUser("approver", "approver123", "王老师");
        createUser("admin", "admin123", "系统管理员");
    }

    private void createUser(String username, String rawPassword, String displayName) {
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setDisplayName(displayName);
        user.setEmail(username + "@usst.edu.cn");
        user.setPhone("13800000000");
        user.setStatus("ACTIVE");
        user.setBookingAllowed(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);

        String roleCode = "user".equals(username)
                ? "USER"
                : "approver".equals(username) ? "APPROVER" : "ADMIN";
        Role role = roleMapper.selectOne(
                Wrappers.<Role>lambdaQuery().eq(Role::getCode, roleCode).last("LIMIT 1")
        );
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
    }

    private void createRooms() {
        Building optical = createBuilding("光电信息楼", "军工路校区", "上海市杨浦区军工路516号");
        Building library = createBuilding("图书馆", "军工路校区", "上海市杨浦区军工路516号");

        Equipment projector = createEquipment("投影仪", "Presentation");
        Equipment video = createEquipment("视频会议", "Video");
        Equipment whiteboard = createEquipment("电子白板", "PenTool");
        Equipment microphone = createEquipment("麦克风", "Mic");

        Room optical301 = createRoom(
                optical.getId(), "301 学术研讨室", 3, "光电楼 A 区 301", 20,
                "/rooms/room-301.jpg",
                "适合课题组讨论和中小型学术交流，自然采光充足。"
        );
        Room optical508 = createRoom(
                optical.getId(), "508 项目会议室", 5, "光电楼 A 区 508", 12,
                "/rooms/room-508.jpg",
                "配备视频会议终端，适合远程答辩和项目评审。"
        );
        Room libraryA = createRoom(
                library.getId(), "A 区多功能厅", 1, "图书馆 A 区一层", 60,
                "/rooms/room-hall.jpg",
                "空间开阔，可移动桌椅，适合讲座和大型研讨。"
        );
        Room libraryB = createRoom(
                library.getId(), "B 区协作室", 2, "图书馆 B 区 206", 8,
                "/rooms/room-collab.jpg",
                "小型协作空间，适合快速讨论和面试。"
        );

        linkEquipment(optical301, projector, video, whiteboard);
        linkEquipment(optical508, projector, video, microphone);
        linkEquipment(libraryA, projector, microphone);
        linkEquipment(libraryB, video, whiteboard);
    }

    private Building createBuilding(String name, String campus, String address) {
        Building building = new Building();
        building.setName(name);
        building.setCampus(campus);
        building.setAddress(address);
        building.setStatus("ACTIVE");
        buildingMapper.insert(building);
        return building;
    }

    private Equipment createEquipment(String name, String icon) {
        Equipment equipment = new Equipment();
        equipment.setName(name);
        equipment.setIcon(icon);
        equipmentMapper.insert(equipment);
        return equipment;
    }

    private Room createRoom(
            Long buildingId,
            String name,
            Integer floor,
            String location,
            Integer capacity,
            String imageUrl,
            String description
    ) {
        LocalDateTime now = LocalDateTime.now();
        Room room = new Room();
        room.setBuildingId(buildingId);
        room.setName(name);
        room.setFloorNo(floor);
        room.setLocation(location);
        room.setCapacity(capacity);
        room.setOpenTime(LocalTime.of(8, 0));
        room.setCloseTime(LocalTime.of(21, 0));
        room.setStatus("AVAILABLE");
        room.setImageUrl(imageUrl);
        room.setDescription(description);
        room.setCreatedAt(now);
        room.setUpdatedAt(now);
        roomMapper.insert(room);
        return room;
    }

    private void linkEquipment(Room room, Equipment... equipment) {
        for (Equipment item : equipment) {
            RoomEquipment link = new RoomEquipment();
            link.setRoomId(room.getId());
            link.setEquipmentId(item.getId());
            link.setQuantity(1);
            roomEquipmentMapper.insert(link);
        }
    }

    private void createDemoBookings() {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, "user").last("LIMIT 1")
        );
        User approver = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, "approver").last("LIMIT 1")
        );
        List<Room> rooms = roomMapper.selectList(
                Wrappers.<Room>lambdaQuery().orderByAsc(Room::getId).last("LIMIT 4")
        );
        if (user == null || approver == null || rooms.size() < 4) {
            return;
        }

        LocalDate today = LocalDate.now();
        Booking approved = createBooking(
                user,
                rooms.get(0),
                "课题组周例会",
                "汇报本周研究进展并讨论下一步实验安排。",
                8,
                today.plusDays(1).atTime(10, 0),
                today.plusDays(1).atTime(11, 30),
                BookingStatus.APPROVED
        );
        createApproval(approved, approver, "APPROVED", "时间与会议室均可用，同意预约。");
        createNotification(
                user.getId(),
                approved.getId(),
                "预约审批结果",
                "预约“课题组周例会”已通过，请按时使用会议室。"
        );

        Booking pending = createBooking(
                user,
                rooms.get(1),
                "远程项目评审",
                "与校外合作单位进行项目阶段评审。",
                10,
                today.plusDays(2).atTime(14, 0),
                today.plusDays(2).atTime(16, 0),
                BookingStatus.PENDING
        );
        createNotification(
                approver.getId(),
                pending.getId(),
                "有新的预约待审批",
                "预约“远程项目评审”等待处理。"
        );

        Booking completed = createBooking(
                user,
                rooms.get(2),
                "创新实践课程讨论",
                "课程小组集中讨论项目分工。",
                24,
                today.minusDays(1).atTime(13, 0),
                today.minusDays(1).atTime(15, 0),
                BookingStatus.COMPLETED
        );
        createApproval(completed, approver, "APPROVED", "同意使用。");
        createNotification(
                approver.getId(),
                completed.getId(),
                "预约已完成",
                "图书馆 A 区多功能厅的预约已结束。"
        );
    }

    private Booking createBooking(
            User user,
            Room room,
            String subject,
            String purpose,
            int attendees,
            LocalDateTime start,
            LocalDateTime end,
            BookingStatus status
    ) {
        LocalDateTime now = LocalDateTime.now();
        Booking booking = new Booking();
        booking.setBookingNo(
                "DEMO" + now.format(DateTimeFormatter.ofPattern("MMddHHmmss"))
                        + UUID.randomUUID().toString().substring(0, 2).toUpperCase(Locale.ROOT)
        );
        booking.setUserId(user.getId());
        booking.setRoomId(room.getId());
        booking.setSubject(subject);
        booking.setPurpose(purpose);
        booking.setAttendeeCount(attendees);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setStatus(status.name());
        booking.setReminderSent(false);
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);
        bookingMapper.insert(booking);
        return booking;
    }

    private void createApproval(
            Booking booking,
            User approver,
            String result,
            String comment
    ) {
        BookingApproval approval = new BookingApproval();
        approval.setBookingId(booking.getId());
        approval.setApproverId(approver.getId());
        approval.setResult(result);
        approval.setComment(comment);
        approval.setCreatedAt(LocalDateTime.now());
        approvalMapper.insert(approval);
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
