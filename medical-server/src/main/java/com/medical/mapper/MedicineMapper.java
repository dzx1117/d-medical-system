package com.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.entity.Medicine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MedicineMapper extends BaseMapper<Medicine> {

    /**
     * 原子扣减库存：仅当库存充足时才扣减，防止并发超卖。
     */
    @Update("UPDATE medicine SET stock = stock - #{quantity} " +
            "WHERE id = #{medicineId} AND stock >= #{quantity} AND deleted = 0")
    int deductStock(@Param("medicineId") Long medicineId, @Param("quantity") Integer quantity);
}
