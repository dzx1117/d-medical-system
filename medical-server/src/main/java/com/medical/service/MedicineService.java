package com.medical.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.entity.Medicine;

import java.util.List;

public interface MedicineService {

    Page<Medicine> page(Integer pageNum, Integer pageSize, String name, String category);

    List<Medicine> listAvailable();

    Medicine getById(Long id);

    void save(Medicine medicine);

    void update(Medicine medicine);

    void delete(Long id);
}
