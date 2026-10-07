package com.usst.meetingroom.service;

import com.usst.meetingroom.domain.AuditLog;
import com.usst.meetingroom.mapper.AuditLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditService {

    private final AuditLogMapper auditLogMapper;

    public AuditService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    public void record(
            Long operatorId,
            String action,
            String targetType,
            String targetId,
            String result,
            String detail
    ) {
        AuditLog log = new AuditLog();
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setResult(result);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(log);
    }
}

