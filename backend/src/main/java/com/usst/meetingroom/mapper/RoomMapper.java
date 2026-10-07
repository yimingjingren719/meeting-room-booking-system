package com.usst.meetingroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usst.meetingroom.domain.Room;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoomMapper extends BaseMapper<Room> {

    @Select("SELECT * FROM room WHERE id = #{id} FOR UPDATE")
    Room selectByIdForUpdate(@Param("id") Long id);
}

