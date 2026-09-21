package com.medical.service;

import com.medical.dto.LoginDTO;
import com.medical.dto.RegisterDTO;
import com.medical.vo.UserVO;

import java.util.Map;

public interface UserService {

    Map<String, Object> login(LoginDTO loginDTO);

    void register(RegisterDTO registerDTO);

    UserVO getCurrentUserInfo(Long userId);
}
