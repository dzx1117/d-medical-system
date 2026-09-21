package com.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {

    /**
     * 原子扣减号源：仅当剩余号源充足时才 +1，返回影响行数。
     * 用于高并发场景下防止超卖（数据库层面的最后一道防线）。
     */
    @Update("UPDATE schedule SET booked_slots = booked_slots + 1 " +
            "WHERE id = #{scheduleId} AND booked_slots < total_slots AND deleted = 0")
    int incrementBookedSlots(@Param("scheduleId") Long scheduleId);

    /**
     * 释放号源（取消预约时调用），booked_slots 不会小于 0。
     */
    @Update("UPDATE schedule SET booked_slots = booked_slots - 1 " +
            "WHERE id = #{scheduleId} AND booked_slots > 0 AND deleted = 0")
    int decrementBookedSlots(@Param("scheduleId") Long scheduleId);
}
