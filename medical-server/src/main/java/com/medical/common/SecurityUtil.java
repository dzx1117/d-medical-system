package com.medical.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 从 SecurityContext 中读取当前登录用户信息。
 * JwtAuthenticationFilter 会把 userId 作为 UserDetails 的 username 写入 principal。
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "未登录或登录已过期");
        }

        Object principal = authentication.getPrincipal();
        String userId;
        if (principal instanceof UserDetails userDetails) {
            userId = userDetails.getUsername();
        } else if (principal instanceof String str && !"anonymousUser".equals(str)) {
            userId = str;
        } else {
            throw new BusinessException(401, "未登录或登录已过期");
        }

        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException e) {
            throw new BusinessException(401, "登录状态异常，请重新登录");
        }
    }
}
