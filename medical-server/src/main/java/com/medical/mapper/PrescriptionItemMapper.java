package com.medical.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.entity.PrescriptionItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PrescriptionItemMapper extends BaseMapper<PrescriptionItem> {
}
