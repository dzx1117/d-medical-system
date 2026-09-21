package com.medical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.entity.Department;
import com.medical.mapper.DepartmentMapper;
import com.medical.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;

    @Override
    public Page<Department> page(Integer pageNum, Integer pageSize, String deptName) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(deptName), Department::getDeptName, deptName)
               .orderByDesc(Department::getCreateTime);
        return departmentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<Department> listAll() {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getStatus, 1).orderByAsc(Department::getDeptCode);
        return departmentMapper.selectList(wrapper);
    }

    @Override
    public Department getById(Long id) {
        Department department = departmentMapper.selectById(id);
        if (department == null) {
            throw new BusinessException("科室不存在");
        }
        return department;
    }

    @Override
    public void save(Department department) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getDeptCode, department.getDeptCode());
        if (departmentMapper.selectOne(wrapper) != null) {
            throw new BusinessException("科室编码已存在");
        }
        if (department.getStatus() == null) {
            department.setStatus(1);
        }
        departmentMapper.insert(department);
    }

    @Override
    public void update(Department department) {
        getById(department.getId());
        departmentMapper.updateById(department);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        departmentMapper.deleteById(id);
    }
}
