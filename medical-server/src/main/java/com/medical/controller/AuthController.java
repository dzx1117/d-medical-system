package com.medical.controller;

import com.medical.common.Result;
import com.medical.dto.LoginDTO;
import com.medical.dto.RegisterDTO;
import com.medical.security.JwtUtil;
import com.medical.service.UserService;
import com.medical.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success(userService.login(loginDTO));
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.successMsg("注册成功");
    }

    @GetMapping("/info")
    public Result<UserVO> info(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);
        return Result.success(userService.getCurrentUserInfo(userId));
    }
}
