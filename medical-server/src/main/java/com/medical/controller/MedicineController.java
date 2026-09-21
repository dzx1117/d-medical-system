package com.medical.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.Result;
import com.medical.entity.Medicine;
import com.medical.service.MedicineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medicine")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    @GetMapping("/page")
    public Result<Page<Medicine>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                       @RequestParam(required = false) String name,
                                       @RequestParam(required = false) String category) {
        return Result.success(medicineService.page(pageNum, pageSize, name, category));
    }

    @GetMapping("/list")
    public Result<List<Medicine>> listAvailable() {
        return Result.success(medicineService.listAvailable());
    }

    @GetMapping("/{id}")
    public Result<Medicine> getById(@PathVariable Long id) {
        return Result.success(medicineService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Medicine medicine) {
        medicineService.save(medicine);
        return Result.successMsg("新增成功");
    }

    @PutMapping
    public Result<Void> update(@RequestBody Medicine medicine) {
        medicineService.update(medicine);
        return Result.successMsg("修改成功");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        medicineService.delete(id);
        return Result.successMsg("删除成功");
    }
}
