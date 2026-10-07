package com.usst.meetingroom.controller;

import com.usst.meetingroom.common.ApiResponse;
import com.usst.meetingroom.dto.BookingCreateRequest;
import com.usst.meetingroom.dto.BookingView;
import com.usst.meetingroom.dto.CancelBookingRequest;
import com.usst.meetingroom.security.UserPrincipal;
import com.usst.meetingroom.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ApiResponse<BookingView> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BookingCreateRequest request
    ) {
        return ApiResponse.success(bookingService.create(principal.getId(), request));
    }

    @GetMapping("/mine")
    public ApiResponse<List<BookingView>> mine(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(bookingService.listMine(principal.getId(), status));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<BookingView> cancel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelBookingRequest request
    ) {
        CancelBookingRequest safeRequest = request == null
                ? new CancelBookingRequest(null)
                : request;
        boolean canManageAll = principal.getRoles().contains("ADMIN");
        return ApiResponse.success(
                bookingService.cancel(principal.getId(), canManageAll, id, safeRequest)
        );
    }
}

