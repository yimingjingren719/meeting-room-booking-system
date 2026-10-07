package com.usst.meetingroom.controller;

import com.usst.meetingroom.common.ApiResponse;
import com.usst.meetingroom.common.BusinessException;
import com.usst.meetingroom.dto.AuthResponse;
import com.usst.meetingroom.dto.ChangePasswordRequest;
import com.usst.meetingroom.dto.LoginRequest;
import com.usst.meetingroom.dto.RegisterRequest;
import com.usst.meetingroom.dto.UpdateProfileRequest;
import com.usst.meetingroom.dto.UserProfile;
import com.usst.meetingroom.security.JwtService;
import com.usst.meetingroom.security.UserPrincipal;
import com.usst.meetingroom.service.AuditService;
import com.usst.meetingroom.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserService userService,
            JwtService jwtService,
            AuditService auditService
    ) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username().trim(),
                            request.password()
                    )
            );
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            String token = jwtService.generateToken(principal);
            auditService.record(
                    principal.getId(),
                    "LOGIN",
                    "USER",
                    principal.getId().toString(),
                    "SUCCESS",
                    "用户登录成功"
            );
            return ApiResponse.success(
                    new AuthResponse(token, userService.profileOf(principal.getId()))
            );
        } catch (DisabledException exception) {
            throw new BusinessException(401, "账号已停用，请联系管理员");
        } catch (AuthenticationException exception) {
            throw new BusinessException(401, "用户名或密码错误");
        }
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserProfile profile = userService.register(request);
        UserPrincipal principal = (UserPrincipal) userService.loadUserByUsername(profile.username());
        return ApiResponse.success(new AuthResponse(jwtService.generateToken(principal), profile));
    }

    @GetMapping("/me")
    public ApiResponse<UserProfile> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(userService.profileOf(principal.getId()));
    }

    @PutMapping("/me")
    public ApiResponse<UserProfile> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ApiResponse.success(userService.updateProfile(principal.getId(), request));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(principal.getId(), request);
        return ApiResponse.success();
    }
}

