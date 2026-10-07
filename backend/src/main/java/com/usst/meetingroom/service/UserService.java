package com.usst.meetingroom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usst.meetingroom.common.BusinessException;
import com.usst.meetingroom.domain.Role;
import com.usst.meetingroom.domain.User;
import com.usst.meetingroom.domain.UserRole;
import com.usst.meetingroom.dto.AdminUserView;
import com.usst.meetingroom.dto.BookingPermissionRequest;
import com.usst.meetingroom.dto.ChangePasswordRequest;
import com.usst.meetingroom.dto.RegisterRequest;
import com.usst.meetingroom.dto.UpdateProfileRequest;
import com.usst.meetingroom.dto.UserProfile;
import com.usst.meetingroom.mapper.RoleMapper;
import com.usst.meetingroom.mapper.UserMapper;
import com.usst.meetingroom.mapper.UserRoleMapper;
import com.usst.meetingroom.security.UserPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(
            UserMapper userMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            PasswordEncoder passwordEncoder,
            AuditService auditService
    ) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        return new UserPrincipal(user, roleMapper.selectCodesByUserId(user.getId()));
    }

    public User findByUsername(String username) {
        return userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username).last("LIMIT 1")
        );
    }

    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    public List<String> rolesOf(Long userId) {
        return roleMapper.selectCodesByUserId(userId);
    }

    public UserProfile profileOf(Long userId) {
        User user = findById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return UserProfile.from(user, rolesOf(userId));
    }

    @Transactional
    public UserProfile register(RegisterRequest request) {
        if (findByUsername(request.username()) != null) {
            throw new BusinessException("用户名已存在，请更换后重试");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(request.username().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setStatus("ACTIVE");
        user.setBookingAllowed(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);

        Role userRoleDefinition = requireRole("USER");

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(userRoleDefinition.getId());
        userRoleMapper.insert(userRole);
        return UserProfile.from(user, List.of("USER"));
    }

    @Transactional
    public UserProfile updateProfile(Long userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        user.setDisplayName(request.displayName().trim());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return UserProfile.from(user, rolesOf(userId));
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException("当前密码不正确");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与当前密码相同");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    public void requireBookingAllowed(Long userId) {
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!Boolean.TRUE.equals(user.getBookingAllowed())) {
            String reason = StringUtils.hasText(user.getBookingBlockReason())
                    ? "，原因：" + user.getBookingBlockReason()
                    : "";
            throw new BusinessException(403, "当前账号已被禁止预约会议室" + reason);
        }
    }

    public List<AdminUserView> listUsers() {
        return userMapper.selectList(
                Wrappers.<User>lambdaQuery().orderByDesc(User::getCreatedAt)
        ).stream().map(user -> new AdminUserView(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus(),
                user.getBookingAllowed(),
                user.getBookingBlockReason(),
                rolesOf(user.getId()),
                user.getCreatedAt()
        )).toList();
    }

    @Transactional
    public void revokeApprover(Long operatorId, Long userId) {
        User user = requireUser(userId);
        List<String> roles = rolesOf(userId);
        if (roles.contains("ADMIN")) {
            throw new BusinessException("不能修改系统管理员角色");
        }
        if (!roles.contains("APPROVER")) {
            throw new BusinessException("该账号不是审批管理员");
        }
        Role approverRole = requireRole("APPROVER");
        userRoleMapper.delete(
                Wrappers.<UserRole>lambdaQuery()
                        .eq(UserRole::getUserId, userId)
                        .eq(UserRole::getRoleId, approverRole.getId())
        );
        if (!roles.contains("USER")) {
            addRole(userId, requireRole("USER"));
        }
        auditService.record(
                operatorId,
                "REVOKE_APPROVER",
                "USER",
                userId.toString(),
                "SUCCESS",
                "撤销审批管理员权限：" + user.getUsername()
        );
    }

    @Transactional
    public void grantApprover(Long operatorId, Long userId) {
        User user = requireUser(userId);
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException("账号已停用，不能恢复审批权限");
        }
        List<String> roles = rolesOf(userId);
        if (roles.contains("ADMIN")) {
            throw new BusinessException("系统管理员已拥有审批权限");
        }
        if (roles.contains("APPROVER")) {
            throw new BusinessException("该账号已经是审批管理员");
        }
        addRole(userId, requireRole("APPROVER"));
        auditService.record(
                operatorId,
                "GRANT_APPROVER",
                "USER",
                userId.toString(),
                "SUCCESS",
                "恢复审批管理员权限：" + user.getUsername()
        );
    }

    @Transactional
    public void disableUserAccount(Long operatorId, Long userId) {
        User user = requireUser(userId);
        List<String> roles = rolesOf(userId);
        if (roles.contains("ADMIN")) {
            throw new BusinessException("不能删除系统管理员账号");
        }
        user.setStatus("DISABLED");
        user.setBookingAllowed(false);
        user.setBookingBlockReason("账号已停用");
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        Role approverRole = requireRole("APPROVER");
        userRoleMapper.delete(
                Wrappers.<UserRole>lambdaQuery()
                        .eq(UserRole::getUserId, userId)
                        .eq(UserRole::getRoleId, approverRole.getId())
        );
        auditService.record(
                operatorId,
                "DELETE_USER",
                "USER",
                userId.toString(),
                "SUCCESS",
                "停用账号：" + user.getUsername()
        );
    }

    @Transactional
    public AdminUserView updateBookingPermission(
            Long operatorId,
            Long userId,
            BookingPermissionRequest request
    ) {
        User user = requireUser(userId);
        List<String> roles = rolesOf(userId);
        if (roles.contains("ADMIN")) {
            throw new BusinessException("不能限制系统管理员的预约权限");
        }
        if (!Boolean.TRUE.equals(request.allowed())
                && !StringUtils.hasText(request.reason())) {
            throw new BusinessException("禁止预约时请填写原因");
        }
        user.setBookingAllowed(request.allowed());
        user.setBookingBlockReason(
                Boolean.TRUE.equals(request.allowed()) ? null : request.reason().trim()
        );
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        auditService.record(
                operatorId,
                "UPDATE_BOOKING_PERMISSION",
                "USER",
                userId.toString(),
                "SUCCESS",
                Boolean.TRUE.equals(request.allowed()) ? "恢复预约权限" : "禁止预约：" + request.reason()
        );
        return new AdminUserView(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus(),
                user.getBookingAllowed(),
                user.getBookingBlockReason(),
                roles,
                user.getCreatedAt()
        );
    }

    private Role requireRole(String code) {
        Role role = roleMapper.selectOne(
                Wrappers.<Role>lambdaQuery()
                        .eq(Role::getCode, code)
                        .last("LIMIT 1")
        );
        if (role == null) {
            throw new BusinessException(500, "系统角色配置不完整，请联系管理员");
        }
        return role;
    }

    private void addRole(Long userId, Role role) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
    }

    private User requireUser(Long userId) {
        User user = findById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }
}

