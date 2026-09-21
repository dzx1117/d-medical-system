package com.medical.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 解析 JWT 并把「用户 ID + 角色」写入 SecurityContext。
 *
 * <p>principal 的用户名位置放 userId（后续用 SecurityUtil 取出），
 * 权限列表按 userType 映射为 ROLE_PATIENT / ROLE_DOCTOR / ROLE_ADMIN，
 * 供 @PreAuthorize 做接口级鉴权。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            try {
                DecodedJWT jwt = jwtUtil.verifyToken(token);
                String userId = jwt.getSubject();
                Integer userType = jwt.getClaim("userType").asInt();

                UserDetails userDetails = new User(
                        userId,
                        "",
                        List.of(new SimpleGrantedAuthority(roleOf(userType))));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(userId);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // token 非法或过期：不设置认证信息，由后续鉴权环节返回 401/403
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String roleOf(Integer userType) {
        if (userType == null) {
            return "ROLE_PATIENT";
        }
        return switch (userType) {
            case 1 -> "ROLE_DOCTOR";
            case 2 -> "ROLE_ADMIN";
            default -> "ROLE_PATIENT";
        };
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
