package com.usst.meetingroom.controller;

import com.usst.meetingroom.common.ApiResponse;
import com.usst.meetingroom.domain.Room;
import com.usst.meetingroom.domain.RoomUnavailable;
import com.usst.meetingroom.dto.AdminUserView;
import com.usst.meetingroom.dto.ApprovalRequest;
import com.usst.meetingroom.dto.AuditLogView;
import com.usst.meetingroom.dto.BookingPermissionRequest;
import com.usst.meetingroom.dto.BookingView;
import com.usst.meetingroom.dto.RoomStatusRequest;
import com.usst.meetingroom.dto.RoomUpsertRequest;
import com.usst.meetingroom.dto.StatsResponse;
import com.usst.meetingroom.dto.UnavailableRequest;
import com.usst.meetingroom.dto.UnavailableView;
import com.usst.meetingroom.security.UserPrincipal;
import com.usst.meetingroom.service.AdminService;
import com.usst.meetingroom.service.BookingService;
import com.usst.meetingroom.service.StatsService;
import com.usst.meetingroom.service.UserService;
import com.usst.meetingroom.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final BookingService bookingService;
    private final StatsService statsService;
    private final AdminService adminService;
    private final UserService userService;
    private final RoomService roomService;

    public AdminController(
            BookingService bookingService,
            StatsService statsService,
            AdminService adminService,
            UserService userService,
            RoomService roomService
    ) {
        this.bookingService = bookingService;
        this.statsService = statsService;
        this.adminService = adminService;
        this.userService = userService;
        this.roomService = roomService;
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ApiResponse<List<BookingView>> bookings(
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(bookingService.listAll(status));
    }

    @PostMapping("/bookings/{id}/approval")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ApiResponse<BookingView> approval(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ApprovalRequest request
    ) {
        return ApiResponse.success(bookingService.approve(principal.getId(), id, request));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN')")
    public ApiResponse<StatsResponse> stats() {
        return ApiResponse.success(statsService.overview());
    }

    @PutMapping("/rooms/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Room> updateRoomStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody RoomStatusRequest request
    ) {
        return ApiResponse.success(
                adminService.updateRoomStatus(principal.getId(), id, request.status())
        );
    }

    @GetMapping("/rooms")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<com.usst.meetingroom.dto.RoomResponse>> rooms() {
        return ApiResponse.success(
                roomService.listRooms(null, null, null, null, null, null, true)
        );
    }

    @PostMapping("/rooms")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Room> createRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RoomUpsertRequest request
    ) {
        return ApiResponse.success(adminService.createRoom(principal.getId(), request));
    }

    @PutMapping("/rooms/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Room> updateRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody RoomUpsertRequest request
    ) {
        return ApiResponse.success(adminService.updateRoom(principal.getId(), id, request));
    }

    @DeleteMapping("/rooms/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> disableRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        adminService.disableRoom(principal.getId(), id);
        return ApiResponse.success();
    }

    @GetMapping("/rooms/unavailable")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UnavailableView>> unavailableRooms() {
        return ApiResponse.success(adminService.listUnavailable());
    }

    @PostMapping("/rooms/unavailable")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RoomUnavailable> createUnavailable(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UnavailableRequest request
    ) {
        return ApiResponse.success(adminService.createUnavailable(principal.getId(), request));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AuditLogView>> auditLogs() {
        return ApiResponse.success(adminService.listAuditLogs());
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AdminUserView>> users() {
        return ApiResponse.success(userService.listUsers());
    }

    @DeleteMapping("/users/{id}/approver")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> revokeApprover(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        userService.revokeApprover(principal.getId(), id);
        return ApiResponse.success();
    }

    @PostMapping("/users/{id}/approver")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> grantApprover(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        userService.grantApprover(principal.getId(), id);
        return ApiResponse.success();
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> disableUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        userService.disableUserAccount(principal.getId(), id);
        return ApiResponse.success();
    }

    @PutMapping("/users/{id}/booking-permission")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminUserView> updateBookingPermission(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody BookingPermissionRequest request
    ) {
        return ApiResponse.success(
                userService.updateBookingPermission(principal.getId(), id, request)
        );
    }
}

