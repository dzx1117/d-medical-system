package com.medical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.entity.Department;
import com.medical.entity.Doctor;
import com.medical.entity.Schedule;
import com.medical.entity.User;
import com.medical.mapper.DepartmentMapper;
import com.medical.mapper.DoctorMapper;
import com.medical.mapper.ScheduleMapper;
import com.medical.mapper.UserMapper;
import com.medical.service.ScheduleService;
import com.medical.vo.ScheduleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleMapper scheduleMapper;
    private final DoctorMapper doctorMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;

    @Override
    public Page<ScheduleVO> page(Integer pageNum, Integer pageSize, Long doctorId,
                                 Long departmentId, LocalDate scheduleDate) {
        LambdaQueryWrapper<Schedule> wrapper = buildWrapper(doctorId, departmentId, scheduleDate);
        wrapper.orderByDesc(Schedule::getScheduleDate);
        Page<Schedule> schedulePage = scheduleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Page<ScheduleVO> voPage = new Page<>(schedulePage.getCurrent(), schedulePage.getSize(), schedulePage.getTotal());
        voPage.setRecords(convertList(schedulePage.getRecords()));
        return voPage;
    }

    @Override
    public List<ScheduleVO> listAvailable(Long departmentId, Long doctorId, LocalDate scheduleDate) {
        LambdaQueryWrapper<Schedule> wrapper = buildWrapper(doctorId, departmentId, scheduleDate);
        wrapper.eq(Schedule::getStatus, 1)
               .ge(scheduleDate == null, Schedule::getScheduleDate, LocalDate.now())
               .apply("booked_slots < total_slots")
               .orderByAsc(Schedule::getScheduleDate, Schedule::getTimeSlot);
        return convertList(scheduleMapper.selectList(wrapper));
    }

    @Override
    public ScheduleVO getVOById(Long id) {
        Schedule schedule = scheduleMapper.selectById(id);
        if (schedule == null) {
            throw new BusinessException("排班不存在");
        }
        return convertList(Collections.singletonList(schedule)).get(0);
    }

    @Override
    public void save(Schedule schedule) {
        Doctor doctor = doctorMapper.selectById(schedule.getDoctorId());
        if (doctor == null) {
            throw new BusinessException("医生不存在");
        }
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Schedule::getDoctorId, schedule.getDoctorId())
               .eq(Schedule::getScheduleDate, schedule.getScheduleDate())
               .eq(Schedule::getTimeSlot, schedule.getTimeSlot());
        if (scheduleMapper.selectOne(wrapper) != null) {
            throw new BusinessException("该医生当天该时段已存在排班");
        }
        schedule.setBookedSlots(0);
        if (schedule.getStatus() == null) {
            schedule.setStatus(1);
        }
        scheduleMapper.insert(schedule);
    }

    @Override
    public void update(Schedule schedule) {
        if (scheduleMapper.selectById(schedule.getId()) == null) {
            throw new BusinessException("排班不存在");
        }
        // 已预约数由系统维护，不允许直接修改
        schedule.setBookedSlots(null);
        scheduleMapper.updateById(schedule);
    }

    @Override
    public void delete(Long id) {
        Schedule schedule = scheduleMapper.selectById(id);
        if (schedule == null) {
            throw new BusinessException("排班不存在");
        }
        if (schedule.getBookedSlots() != null && schedule.getBookedSlots() > 0) {
            throw new BusinessException("该排班已有患者预约，无法删除");
        }
        scheduleMapper.deleteById(id);
    }

    private LambdaQueryWrapper<Schedule> buildWrapper(Long doctorId, Long departmentId, LocalDate scheduleDate) {
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(scheduleDate != null, Schedule::getScheduleDate, scheduleDate);

        if (doctorId != null) {
            wrapper.eq(Schedule::getDoctorId, doctorId);
        } else if (departmentId != null) {
            List<Long> doctorIds = doctorMapper.selectList(
                    new LambdaQueryWrapper<Doctor>().eq(Doctor::getDepartmentId, departmentId))
                    .stream().map(Doctor::getId).collect(Collectors.toList());
            if (doctorIds.isEmpty()) {
                // 无医生时返回不可能匹配的条件，避免查出全表
                wrapper.eq(Schedule::getId, -1L);
            } else {
                wrapper.in(Schedule::getDoctorId, doctorIds);
            }
        }
        return wrapper;
    }

    private List<ScheduleVO> convertList(List<Schedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> doctorIds = schedules.stream().map(Schedule::getDoctorId).distinct().collect(Collectors.toList());
        List<Doctor> doctors = doctorMapper.selectBatchIds(doctorIds);
        Map<Long, Doctor> doctorMap = doctors.stream().collect(Collectors.toMap(Doctor::getId, Function.identity()));

        List<Long> userIds = doctors.stream().map(Doctor::getUserId).collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<Long> deptIds = doctors.stream().map(Doctor::getDepartmentId).distinct().collect(Collectors.toList());
        Map<Long, Department> deptMap = departmentMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(Department::getId, Function.identity()));

        return schedules.stream().map(schedule -> {
            ScheduleVO vo = new ScheduleVO();
            vo.setId(schedule.getId());
            vo.setDoctorId(schedule.getDoctorId());
            vo.setScheduleDate(schedule.getScheduleDate());
            vo.setTimeSlot(schedule.getTimeSlot());
            vo.setTimeSlotDesc(timeSlotDesc(schedule.getTimeSlot()));
            vo.setTotalSlots(schedule.getTotalSlots());
            vo.setBookedSlots(schedule.getBookedSlots());
            vo.setRemainingSlots(schedule.getTotalSlots() - schedule.getBookedSlots());
            vo.setFee(schedule.getFee());
            vo.setStatus(schedule.getStatus());
            vo.setCreateTime(schedule.getCreateTime());

            Doctor doctor = doctorMap.get(schedule.getDoctorId());
            if (doctor != null) {
                vo.setTitle(doctor.getTitle());
                vo.setDepartmentId(doctor.getDepartmentId());
                User user = userMap.get(doctor.getUserId());
                if (user != null) {
                    vo.setDoctorName(user.getRealName());
                }
                Department department = deptMap.get(doctor.getDepartmentId());
                if (department != null) {
                    vo.setDepartmentName(department.getDeptName());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private String timeSlotDesc(Integer timeSlot) {
        if (timeSlot == null) {
            return "";
        }
        return switch (timeSlot) {
            case 1 -> "上午";
            case 2 -> "下午";
            case 3 -> "晚上";
            default -> "未知";
        };
    }
}
