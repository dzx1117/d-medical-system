package com.medical.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.entity.Department;

import java.util.List;

public interface DepartmentService {

    Page<Department> page(Integer pageNum, Integer pageSize, String deptName);

    List<Department> listAll();

    Department getById(Long id);

    void save(Department department);

    void update(Department department);

    void delete(Long id);
}
