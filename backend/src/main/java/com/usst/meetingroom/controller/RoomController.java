package com.usst.meetingroom.controller;

import com.usst.meetingroom.common.ApiResponse;
import com.usst.meetingroom.domain.Building;
import com.usst.meetingroom.domain.Equipment;
import com.usst.meetingroom.dto.AvailabilitySlot;
import com.usst.meetingroom.dto.RoomResponse;
import com.usst.meetingroom.security.UserPrincipal;
import com.usst.meetingroom.service.RoomService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public ApiResponse<List<RoomResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
            @RequestParam(defaultValue = "false") boolean includeDisabled
    ) {
        return ApiResponse.success(
                roomService.listRooms(
                        principal.getId(),
                        buildingId,
                        capacity,
                        equipment,
                        date,
                        startTime,
                        endTime,
                        includeDisabled
                )
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<RoomResponse> detail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(roomService.getRoom(id, date, principal.getId()));
    }

    @GetMapping("/recommendations")
    public ApiResponse<List<RoomResponse>> recommendations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Integer attendeeCount,
            @RequestParam(required = false) String equipment,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime endTime
    ) {
        return ApiResponse.success(
                roomService.recommend(
                        principal.getId(),
                        attendeeCount,
                        equipment,
                        date,
                        startTime,
                        endTime
                )
        );
    }

    @PostMapping("/{id}/favorite")
    public ApiResponse<Void> addFavorite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        roomService.addFavorite(principal.getId(), id);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}/favorite")
    public ApiResponse<Void> removeFavorite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        roomService.removeFavorite(principal.getId(), id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/availability")
    public ApiResponse<List<AvailabilitySlot>> availability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(roomService.availabilityOf(id, date));
    }

    @GetMapping("/options/buildings")
    public ApiResponse<List<Building>> buildings() {
        return ApiResponse.success(roomService.listBuildings());
    }

    @GetMapping("/options/equipment")
    public ApiResponse<List<Equipment>> equipment() {
        return ApiResponse.success(roomService.listEquipment());
    }
}
