package com.usst.meetingroom.dto;

import com.usst.meetingroom.domain.User;

import java.util.List;

public record UserProfile(
        Long id,
        String username,
        String displayName,
        String email,
        String phone,
        Boolean bookingAllowed,
        String bookingBlockReason,
        List<String> roles
) {
    public static UserProfile from(User user, List<String> roles) {
        return new UserProfile(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPhone(),
                user.getBookingAllowed(),
                user.getBookingBlockReason(),
                roles
        );
    }
}

