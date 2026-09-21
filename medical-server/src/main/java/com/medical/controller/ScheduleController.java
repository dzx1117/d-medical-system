package com.medical.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.Result;
import com.medical.entity.Schedule;
import com.medical.service.ScheduleService;
import com.medical.vo.ScheduleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/page")
    public Result<Page<ScheduleVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                         @RequestParam(required = false) Long doctorId,
                                         @RequestParam(required = false) Long departmentId,
                                         @RequestParam(required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduleDate) {
        return Result.success(scheduleService.page(pageNum, pageSize, doctorId, departmentId, scheduleDate));
    }

    /**
     * 患者端：查询可预约号源
     */
    @GetMapping("/available")
    public Result<List<ScheduleVO>> available(@RequestParam(required = false) Long departmentId,
                                              @RequestParam(required = false) Long doctorId,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduleDate) {
        return Result.success(scheduleService.listAvailable(departmentId, doctorId, scheduleDate));
    }

    @GetMapping("/{id}")
    public Result<ScheduleVO> getById(@PathVariable Long id) {
        return Result.success(scheduleService.getVOById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Schedule schedule) {
        scheduleService.save(schedule);
        return Result.successMsg("排班成功");
    }

    @PutMapping
    public Result<Void> update(@RequestBody Schedule schedule) {
        scheduleService.update(schedule);
        return Result.successMsg("修改成功");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return Result.successMsg("删除成功");
    }
}
