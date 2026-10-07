package com.usst.meetingroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usst.meetingroom.domain.Booking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface BookingMapper extends BaseMapper<Booking> {

    @Select("SELECT * FROM booking WHERE id = #{id} FOR UPDATE")
    Booking selectByIdForUpdate(@Param("id") Long id);

    @Select("""
            SELECT COUNT(*)
            FROM booking
            WHERE room_id = #{roomId}
              AND status IN ('PENDING', 'APPROVED')
              AND start_time < #{endTime}
              AND end_time > #{startTime}
            """)
    long countConflicts(
            @Param("roomId") Long roomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Select("""
            SELECT COUNT(*)
            FROM booking
            WHERE room_id = #{roomId}
              AND id <> #{bookingId}
              AND status IN ('PENDING', 'APPROVED')
              AND start_time < #{endTime}
              AND end_time > #{startTime}
            """)
    long countConflictsExcluding(
            @Param("roomId") Long roomId,
            @Param("bookingId") Long bookingId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}

