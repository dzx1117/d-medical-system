package com.medical.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 *
 * <p>校验分两步：先验签名与有效期，再查 Redis 黑名单——后者用于让「退出登录」
 * 立即失效（JWT 本身无状态，光靠前端删 token 是删不干净的）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            try {
                DecodedJWT jwt = jwtUtil.verifyToken(token);

                // 已退出登录的 token：不写入认证信息，后续鉴权环节会返回 401
                if (isRevoked(token)) {
                    log.debug("token 已在黑名单中（用户已退出登录），拒绝认证");
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

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

    /**
     * 查询黑名单。
     *
     * <p>Redis 不可用时选择「放行」而非「全部拒绝」：黑名单是一道补充防线，
     * 放行的 token 仍然通过了签名与有效期校验；如果这里 fail-close，
     * 一次 Redis 抖动就会让全站所有已登录用户变成 401。
     */
    private boolean isRevoked(String token) {
        try {
            return tokenBlacklist.isRevoked(token);
        } catch (Exception e) {
            log.warn("查询 token 黑名单失败，本次跳过黑名单校验：{}", e.getMessage());
            return false;
        }
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
