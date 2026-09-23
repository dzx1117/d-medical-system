package com.medical.controller;

import com.medical.common.Result;
import com.medical.common.SecurityUtil;
import com.medical.dto.LoginDTO;
import com.medical.dto.RegisterDTO;
import com.medical.security.TokenBlacklist;
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
    private final TokenBlacklist tokenBlacklist;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success(userService.login(loginDTO));
    }

    /**
     * 退出登录：把当前 token 加入 Redis 黑名单，使其立即失效。
     *
     * <p>只靠前端删 localStorage 是删不干净的——token 本身仍然有效，
     * 谁拿到都能继续调接口，直到 24 小时有效期结束。
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            tokenBlacklist.revoke(authHeader.substring(7));
        }
        return Result.successMsg("已退出登录");
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.successMsg("注册成功");
    }

    /**
     * 当前登录用户信息。
     *
     * <p>从 SecurityContext 取 userId，而不是自己解析请求头里的 token——
     * 自己解析等于绕过了认证过滤器，被拉黑的 token 照样能拿到用户信息。
     */
    @GetMapping("/info")
    public Result<UserVO> info() {
        return Result.success(userService.getCurrentUserInfo(SecurityUtil.getCurrentUserId()));
    }
}
