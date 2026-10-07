package com.usst.meetingroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usst.meetingroom.domain.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}

