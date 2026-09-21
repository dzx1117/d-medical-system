package com.medical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.entity.Medicine;
import com.medical.mapper.MedicineMapper;
import com.medical.service.MedicineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicineServiceImpl implements MedicineService {

    private final MedicineMapper medicineMapper;

    @Override
    public Page<Medicine> page(Integer pageNum, Integer pageSize, String name, String category) {
        LambdaQueryWrapper<Medicine> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), Medicine::getName, name)
               .eq(StringUtils.hasText(category), Medicine::getCategory, category)
               .orderByDesc(Medicine::getCreateTime);
        return medicineMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<Medicine> listAvailable() {
        LambdaQueryWrapper<Medicine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Medicine::getStatus, 1)
               .gt(Medicine::getStock, 0)
               .orderByAsc(Medicine::getCode);
        return medicineMapper.selectList(wrapper);
    }

    @Override
    public Medicine getById(Long id) {
        Medicine medicine = medicineMapper.selectById(id);
        if (medicine == null) {
            throw new BusinessException("药品不存在");
        }
        return medicine;
    }

    @Override
    public void save(Medicine medicine) {
        LambdaQueryWrapper<Medicine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Medicine::getCode, medicine.getCode());
        if (medicineMapper.selectOne(wrapper) != null) {
            throw new BusinessException("药品编码已存在");
        }
        if (medicine.getStatus() == null) {
            medicine.setStatus(1);
        }
        if (medicine.getStock() == null) {
            medicine.setStock(0);
        }
        medicineMapper.insert(medicine);
    }

    @Override
    public void update(Medicine medicine) {
        getById(medicine.getId());
        medicineMapper.updateById(medicine);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        medicineMapper.deleteById(id);
    }
}
