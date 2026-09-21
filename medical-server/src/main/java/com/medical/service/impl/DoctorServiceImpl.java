package com.medical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.BusinessException;
import com.medical.dto.DoctorDTO;
import com.medical.entity.Department;
import com.medical.entity.Doctor;
import com.medical.entity.User;
import com.medical.mapper.DepartmentMapper;
import com.medical.mapper.DoctorMapper;
import com.medical.mapper.UserMapper;
import com.medical.service.DoctorService;
import com.medical.vo.DoctorVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorMapper doctorMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<DoctorVO> page(Integer pageNum, Integer pageSize, String realName, Long departmentId) {
        LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(departmentId != null, Doctor::getDepartmentId, departmentId)
               .orderByDesc(Doctor::getCreateTime);

        // 按医生姓名搜索时，先查出匹配的用户ID
        if (StringUtils.hasText(realName)) {
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.like(User::getRealName, realName).eq(User::getUserType, 1);
            List<Long> userIds = userMapper.selectList(userWrapper).stream()
                    .map(User::getId).collect(Collectors.toList());
            if (userIds.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
            wrapper.in(Doctor::getUserId, userIds);
        }

        Page<Doctor> doctorPage = doctorMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return convertPage(doctorPage);
    }

    @Override
    public List<DoctorVO> listByDepartment(Long departmentId) {
        LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Doctor::getStatus, 1)
               .eq(departmentId != null, Doctor::getDepartmentId, departmentId);
        List<Doctor> doctors = doctorMapper.selectList(wrapper);
        return convertList(doctors);
    }

    @Override
    public DoctorVO getVOById(Long id) {
        Doctor doctor = doctorMapper.selectById(id);
        if (doctor == null) {
            throw new BusinessException("医生不存在");
        }
        return convertList(Collections.singletonList(doctor)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(DoctorDTO dto) {
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectOne(userWrapper) != null) {
            throw new BusinessException("用户名已存在");
        }
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new BusinessException("请设置初始密码");
        }
        checkDepartment(dto.getDepartmentId());

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setUserType(1);
        user.setStatus(1);
        userMapper.insert(user);

        Doctor doctor = new Doctor();
        doctor.setUserId(user.getId());
        doctor.setDepartmentId(dto.getDepartmentId());
        doctor.setTitle(dto.getTitle());
        doctor.setSpecialty(dto.getSpecialty());
        doctor.setIntroduction(dto.getIntroduction());
        doctor.setFee(dto.getFee() == null ? BigDecimal.ZERO : dto.getFee());
        doctor.setStatus(1);
        doctorMapper.insert(doctor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DoctorDTO dto) {
        Doctor doctor = doctorMapper.selectById(dto.getId());
        if (doctor == null) {
            throw new BusinessException("医生不存在");
        }
        checkDepartment(dto.getDepartmentId());

        User user = userMapper.selectById(doctor.getUserId());
        if (user != null) {
            user.setRealName(dto.getRealName());
            user.setPhone(dto.getPhone());
            userMapper.updateById(user);
        }

        doctor.setDepartmentId(dto.getDepartmentId());
        doctor.setTitle(dto.getTitle());
        doctor.setSpecialty(dto.getSpecialty());
        doctor.setIntroduction(dto.getIntroduction());
        if (dto.getFee() != null) {
            doctor.setFee(dto.getFee());
        }
        doctorMapper.updateById(doctor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Doctor doctor = doctorMapper.selectById(id);
        if (doctor == null) {
            throw new BusinessException("医生不存在");
        }
        doctorMapper.deleteById(id);
        userMapper.deleteById(doctor.getUserId());
    }

    private void checkDepartment(Long departmentId) {
        Department department = departmentMapper.selectById(departmentId);
        if (department == null) {
            throw new BusinessException("所选科室不存在");
        }
    }

    private Page<DoctorVO> convertPage(Page<Doctor> doctorPage) {
        Page<DoctorVO> voPage = new Page<>(doctorPage.getCurrent(), doctorPage.getSize(), doctorPage.getTotal());
        voPage.setRecords(convertList(doctorPage.getRecords()));
        return voPage;
    }

    private List<DoctorVO> convertList(List<Doctor> doctors) {
        if (doctors == null || doctors.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = doctors.stream().map(Doctor::getUserId).collect(Collectors.toList());
        List<Long> deptIds = doctors.stream().map(Doctor::getDepartmentId).distinct().collect(Collectors.toList());

        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Department> deptMap = departmentMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(Department::getId, Function.identity()));

        return doctors.stream().map(doctor -> {
            DoctorVO vo = new DoctorVO();
            vo.setId(doctor.getId());
            vo.setUserId(doctor.getUserId());
            vo.setDepartmentId(doctor.getDepartmentId());
            vo.setTitle(doctor.getTitle());
            vo.setSpecialty(doctor.getSpecialty());
            vo.setIntroduction(doctor.getIntroduction());
            vo.setFee(doctor.getFee());
            vo.setStatus(doctor.getStatus());
            vo.setCreateTime(doctor.getCreateTime());

            User user = userMap.get(doctor.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setRealName(user.getRealName());
                vo.setPhone(user.getPhone());
            }
            Department department = deptMap.get(doctor.getDepartmentId());
            if (department != null) {
                vo.setDepartmentName(department.getDeptName());
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
