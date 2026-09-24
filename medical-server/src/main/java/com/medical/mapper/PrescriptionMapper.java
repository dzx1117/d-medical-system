package com.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.entity.Prescription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PrescriptionMapper extends BaseMapper<Prescription> {

    /**
     * 原子缴费：仅当状态为「待缴费」时才改为「已缴费」，返回影响行数。
     *
     * <p>把状态判断写进 WHERE 条件，而不是先查出来再判断，是为了避免「先读后写」
     * 之间的空隙：两个并发请求都会读到 status=0 并双双通过校验，各写一次。
     * 目前 status 只是个状态位，改两次结果一样、看不出问题；但如果这里换成真实支付，
     * 那就是同一张处方被重复扣款。
     *
     * <p>影响行数为 0 即表示状态已被其他请求改过，调用方应当拒绝本次操作。
     */
    @Update("UPDATE prescription SET status = 1 WHERE id = #{id} AND status = 0")
    int payIfPending(@Param("id") Long id);

    /**
     * 原子取药：仅当状态为「已缴费」时才改为「已取药」，返回影响行数。
     * 与 {@link #payIfPending} 同理，防止并发下重复取药。
     */
    @Update("UPDATE prescription SET status = 2 WHERE id = #{id} AND status = 1")
    int pickupIfPaid(@Param("id") Long id);
}
