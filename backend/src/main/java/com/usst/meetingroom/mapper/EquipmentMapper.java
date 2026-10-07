package com.usst.meetingroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usst.meetingroom.domain.Equipment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EquipmentMapper extends BaseMapper<Equipment> {

    @Select("""
            SELECT e.*
            FROM equipment e
            JOIN room_equipment re ON re.equipment_id = e.id
            WHERE re.room_id = #{roomId}
            ORDER BY e.id
            """)
    List<Equipment> selectByRoomId(@Param("roomId") Long roomId);
}

