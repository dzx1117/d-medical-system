package com.medical.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.dto.DoctorDTO;
import com.medical.vo.DoctorVO;

import java.util.List;

public interface DoctorService {

    Page<DoctorVO> page(Integer pageNum, Integer pageSize, String realName, Long departmentId);

    List<DoctorVO> listByDepartment(Long departmentId);

    DoctorVO getVOById(Long id);

    void save(DoctorDTO doctorDTO);

    void update(DoctorDTO doctorDTO);

    void delete(Long id);
}
