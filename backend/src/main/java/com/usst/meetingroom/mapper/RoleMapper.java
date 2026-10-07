package com.usst.meetingroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usst.meetingroom.domain.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("""
            SELECT r.code
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            ORDER BY r.id
            """)
    List<String> selectCodesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT ur.user_id
            FROM sys_user_role ur
            JOIN sys_role r ON r.id = ur.role_id
            WHERE r.code IN ('APPROVER', 'ADMIN')
            """)
    List<Long> selectApproverIds();
}

