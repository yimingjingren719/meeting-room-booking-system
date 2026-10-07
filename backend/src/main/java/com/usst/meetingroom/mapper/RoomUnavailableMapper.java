package com.usst.meetingroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usst.meetingroom.domain.RoomUnavailable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface RoomUnavailableMapper extends BaseMapper<RoomUnavailable> {

    @Select("""
            SELECT COUNT(*)
            FROM room_unavailable
            WHERE room_id = #{roomId}
              AND start_time < #{endTime}
              AND end_time > #{startTime}
            """)
    long countOverlapping(
            @Param("roomId") Long roomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}

