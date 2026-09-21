package com.medical.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.Result;
import com.medical.entity.Department;
import com.medical.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/department")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping("/page")
    public Result<Page<Department>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                         @RequestParam(required = false) String deptName) {
        return Result.success(departmentService.page(pageNum, pageSize, deptName));
    }

    @GetMapping("/list")
    public Result<List<Department>> listAll() {
        return Result.success(departmentService.listAll());
    }

    @GetMapping("/{id}")
    public Result<Department> getById(@PathVariable Long id) {
        return Result.success(departmentService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Department department) {
        departmentService.save(department);
        return Result.successMsg("新增成功");
    }

    @PutMapping
    public Result<Void> update(@RequestBody Department department) {
        departmentService.update(department);
        return Result.successMsg("修改成功");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return Result.successMsg("删除成功");
    }
}
