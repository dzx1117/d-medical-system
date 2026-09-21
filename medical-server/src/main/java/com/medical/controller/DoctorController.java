package com.medical.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.Result;
import com.medical.dto.DoctorDTO;
import com.medical.service.DoctorService;
import com.medical.vo.DoctorVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping("/page")
    public Result<Page<DoctorVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                       @RequestParam(required = false) String realName,
                                       @RequestParam(required = false) Long departmentId) {
        return Result.success(doctorService.page(pageNum, pageSize, realName, departmentId));
    }

    @GetMapping("/list")
    public Result<List<DoctorVO>> listByDepartment(@RequestParam(required = false) Long departmentId) {
        return Result.success(doctorService.listByDepartment(departmentId));
    }

    @GetMapping("/{id}")
    public Result<DoctorVO> getById(@PathVariable Long id) {
        return Result.success(doctorService.getVOById(id));
    }

    @PostMapping
    public Result<Void> save(@Valid @RequestBody DoctorDTO doctorDTO) {
        doctorService.save(doctorDTO);
        return Result.successMsg("新增成功");
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody DoctorDTO doctorDTO) {
        doctorService.update(doctorDTO);
        return Result.successMsg("修改成功");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        doctorService.delete(id);
        return Result.successMsg("删除成功");
    }
}
