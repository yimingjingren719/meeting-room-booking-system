package com.usst.meetingroom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.usst.meetingroom.domain.Booking;
import com.usst.meetingroom.domain.BookingStatus;
import com.usst.meetingroom.mapper.BookingMapper;
import com.usst.meetingroom.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookingFlowIntegrationTest {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    private BookingService bookingService;

    @Test
    void rejectsOverlappingBooking() throws Exception {
        String token = login("user", "user123");
        LocalDate date = LocalDate.now().plusDays(1);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roomId", 1,
                                "subject", "冲突检测测试",
                                "purpose", "验证同一会议室重叠时段不可重复预约",
                                "attendeeCount", 4,
                                "startTime", format(date.atTime(10, 15)),
                                "endTime", format(date.atTime(10, 45))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("已被其他预约占用")));
    }

    @Test
    void completesCreateApproveAndCancelFlow() throws Exception {
        String userToken = login("user", "user123");
        String approverToken = login("approver", "approver123");
        LocalDate date = LocalDate.now().plusDays(1);

        String createResponse = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roomId", 4,
                                "subject", "集成测试预约",
                                "purpose", "覆盖提交、审批和取消主流程",
                                "attendeeCount", 5,
                                "startTime", format(date.atTime(18, 0)),
                                "endTime", format(date.atTime(19, 0))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createJson = objectMapper.readTree(createResponse);
        long bookingId = createJson.path("data").path("id").asLong();

        mockMvc.perform(post("/api/admin/bookings/{id}/approval", bookingId)
                        .header("Authorization", "Bearer " + approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "approved", true,
                                "comment", "集成测试审批通过"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(post("/api/bookings/{id}/cancel", bookingId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "集成测试完成"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + approverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void rejectsMaintenanceOverlappingActiveBooking() throws Exception {
        String adminToken = login("admin", "admin123");
        LocalDate date = LocalDate.now().plusDays(1);

        mockMvc.perform(post("/api/admin/rooms/unavailable")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roomId", 1,
                                "startTime", format(date.atTime(10, 0)),
                                "endTime", format(date.atTime(11, 0)),
                                "reason", "维护冲突测试"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("已有有效预约")));
    }

    @Test
    void rejectsClosingRoomWithFutureActiveBooking() throws Exception {
        String adminToken = login("admin", "admin123");

        mockMvc.perform(put("/api/admin/rooms/1/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "MAINTENANCE"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("仍有未结束的有效预约")));
    }

    @Test
    void listsCreatedMaintenancePeriod() throws Exception {
        String adminToken = login("admin", "admin123");
        LocalDate date = LocalDate.now().plusDays(1);

        mockMvc.perform(post("/api/admin/rooms/unavailable")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roomId", 4,
                                "startTime", format(date.atTime(18, 0)),
                                "endTime", format(date.atTime(19, 0)),
                                "reason", "维护列表测试"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/admin/rooms/unavailable")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].roomName").value("B 区协作室"))
                .andExpect(jsonPath("$.data[0].reason").value("维护列表测试"));
    }

    @Test
    void userUpdatesProfileAndPassword() throws Exception {
        String token = login("user", "user123");

        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "displayName", "李同学更新",
                                "email", "updated@usst.edu.cn",
                                "phone", "13900000000"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.displayName").value("李同学更新"))
                .andExpect(jsonPath("$.data.email").value("updated@usst.edu.cn"));

        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "user123",
                                "newPassword", "user456"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        login("user", "user456");
    }

    @Test
    void adminBlocksAndRestoresBookingPermission() throws Exception {
        String adminToken = login("admin", "admin123");
        String userToken = login("user", "user123");
        LocalDate date = LocalDate.now().plusDays(1);

        mockMvc.perform(put("/api/admin/users/1/booking-permission")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "allowed", false,
                                "reason", "预约违规测试"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.bookingAllowed").value(false));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roomId", 4,
                                "subject", "权限限制测试",
                                "purpose", "验证禁止预约",
                                "attendeeCount", 4,
                                "startTime", format(date.atTime(18, 0)),
                                "endTime", format(date.atTime(19, 0))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message", containsString("禁止预约")));

        mockMvc.perform(put("/api/admin/users/1/booking-permission")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "allowed", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingAllowed").value(true));
    }

    @Test
    void adminGrantsRevokesAndRegrantsApprover() throws Exception {
        String adminToken = login("admin", "admin123");
        String userToken = login("user", "user123");
        long userId = 1L;

        mockMvc.perform(post("/api/admin/users/{id}/approver", userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[1]").value("APPROVER"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/admin/users/{id}/approver", userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles.length()").value(1));

        mockMvc.perform(post("/api/admin/users/{id}/approver", userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[1]").value("APPROVER"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/admin/users/{id}", userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "user",
                                "password", "user123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message", containsString("停用")));
    }

    @Test
    void adminCreatesUpdatesAndDisablesRoom() throws Exception {
        String adminToken = login("admin", "admin123");
        String userToken = login("user", "user123");
        Map<String, Object> roomRequest = new java.util.HashMap<>();
        roomRequest.put("buildingId", 1);
        roomRequest.put("name", "602 创新协作室");
        roomRequest.put("floorNo", 6);
        roomRequest.put("location", "光电楼 A 区 602");
        roomRequest.put("capacity", 14);
        roomRequest.put("openTime", "08:00:00");
        roomRequest.put("closeTime", "21:00:00");
        roomRequest.put("status", "AVAILABLE");
        roomRequest.put("imageUrl", "/rooms/room-collab.jpg");
        roomRequest.put("description", "用于创新项目讨论");
        roomRequest.put("equipmentIds", List.of(1, 2));

        String response = mockMvc.perform(post("/api/admin/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("602 创新协作室"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long roomId = objectMapper.readTree(response).path("data").path("id").asLong();

        Map<String, Object> updateRequest = new java.util.HashMap<>(roomRequest);
        updateRequest.put("capacity", 18);
        mockMvc.perform(put("/api/admin/rooms/{id}", roomId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capacity").value(18));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/admin/rooms/{id}", roomId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/rooms")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + roomId + ")]").isEmpty());

        mockMvc.perform(get("/api/admin/rooms")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + roomId + ")]").isNotEmpty());
    }

    @Test
    void favoriteAndRecommendationWork() throws Exception {
        String token = login("user", "user123");
        LocalDate date = LocalDate.now().plusDays(1);

        mockMvc.perform(post("/api/rooms/4/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/rooms/4")
                        .header("Authorization", "Bearer " + token)
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorite").value(true));

        mockMvc.perform(get("/api/rooms/recommendations")
                        .header("Authorization", "Bearer " + token)
                        .param("attendeeCount", "5")
                        .param("date", date.toString())
                        .param("startTime", "18:00")
                        .param("endTime", "19:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data[0].recommendationScore").isNumber());
    }

    @Test
    void approvalRejectsLegacyOverlappingApprovedBooking() throws Exception {
        String approverToken = login("approver", "approver123");
        LocalDate date = LocalDate.now().plusDays(1);

        Booking approved = new Booking();
        approved.setBookingNo("LEGACY-APPROVED");
        approved.setUserId(1L);
        approved.setRoomId(4L);
        approved.setSubject("历史已通过预约");
        approved.setPurpose("模拟历史数据");
        approved.setAttendeeCount(4);
        approved.setStartTime(date.atTime(18, 0));
        approved.setEndTime(date.atTime(19, 0));
        approved.setStatus(BookingStatus.APPROVED.name());
        approved.setCreatedAt(LocalDateTime.now());
        approved.setUpdatedAt(LocalDateTime.now());
        bookingMapper.insert(approved);

        Booking pending = new Booking();
        pending.setBookingNo("LEGACY-PENDING");
        pending.setUserId(1L);
        pending.setRoomId(4L);
        pending.setSubject("历史待审批冲突预约");
        pending.setPurpose("模拟历史数据");
        pending.setAttendeeCount(4);
        pending.setStartTime(date.atTime(18, 30));
        pending.setEndTime(date.atTime(19, 30));
        pending.setStatus(BookingStatus.PENDING.name());
        pending.setCreatedAt(LocalDateTime.now());
        pending.setUpdatedAt(LocalDateTime.now());
        bookingMapper.insert(pending);

        mockMvc.perform(post("/api/admin/bookings/{id}/approval", pending.getId())
                        .header("Authorization", "Bearer " + approverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "approved", true,
                                "comment", "审批冲突验证"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message", containsString("已被其他预约占用")));
    }

    @Test
    void sendsUpcomingBookingReminderOnce() {
        LocalDateTime now = LocalDateTime.now();
        Booking booking = new Booking();
        booking.setBookingNo("REMINDER-TEST");
        booking.setUserId(1L);
        booking.setRoomId(4L);
        booking.setSubject("会议提醒测试");
        booking.setPurpose("验证开始前提醒");
        booking.setAttendeeCount(3);
        booking.setStartTime(now.plusMinutes(20));
        booking.setEndTime(now.plusMinutes(50));
        booking.setStatus(BookingStatus.APPROVED.name());
        booking.setReminderSent(false);
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);
        bookingMapper.insert(booking);

        assertTrue(bookingService.sendUpcomingReminders() >= 1);
        assertTrue(bookingMapper.selectById(booking.getId()).getReminderSent());
        assertTrue(bookingService.sendUpcomingReminders() == 0);
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private String format(LocalDateTime value) {
        return value.format(TIME_FORMAT);
    }
}
