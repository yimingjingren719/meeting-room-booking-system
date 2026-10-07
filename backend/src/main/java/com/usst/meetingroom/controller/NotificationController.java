package com.usst.meetingroom.controller;

import com.usst.meetingroom.common.ApiResponse;
import com.usst.meetingroom.dto.NotificationView;
import com.usst.meetingroom.security.UserPrincipal;
import com.usst.meetingroom.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<List<NotificationView>> list(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.success(notificationService.list(principal.getId()));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.success(notificationService.unreadCount(principal.getId()));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> markRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        notificationService.markRead(principal.getId(), id);
        return ApiResponse.success();
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> markAllRead(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        notificationService.markAllRead(principal.getId());
        return ApiResponse.success();
    }
}

