package com.usst.meetingroom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usst.meetingroom.common.BusinessException;
import com.usst.meetingroom.domain.Notification;
import com.usst.meetingroom.dto.NotificationView;
import com.usst.meetingroom.mapper.NotificationMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    public List<NotificationView> list(Long userId) {
        return notificationMapper.selectList(
                Wrappers.<Notification>lambdaQuery()
                        .eq(Notification::getRecipientId, userId)
                        .orderByDesc(Notification::getCreatedAt)
        ).stream().map(this::toView).toList();
    }

    public long unreadCount(Long userId) {
        return notificationMapper.selectCount(
                Wrappers.<Notification>lambdaQuery()
                        .eq(Notification::getRecipientId, userId)
                        .eq(Notification::getReadFlag, false)
        );
    }

    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException(404, "通知不存在");
        }
        if (!notification.getRecipientId().equals(userId)) {
            throw new BusinessException(403, "无权操作该通知");
        }
        notification.setReadFlag(true);
        notificationMapper.updateById(notification);
    }

    public void markAllRead(Long userId) {
        notificationMapper.update(
                null,
                Wrappers.<Notification>lambdaUpdate()
                        .eq(Notification::getRecipientId, userId)
                        .eq(Notification::getReadFlag, false)
                        .set(Notification::getReadFlag, true)
        );
    }

    private NotificationView toView(Notification notification) {
        return new NotificationView(
                notification.getId(),
                notification.getBookingId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getReadFlag(),
                notification.getCreatedAt()
        );
    }
}

