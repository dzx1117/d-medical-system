package com.medical.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * {@link RateLimit} 的切面实现，基于 Redis 做固定窗口计数限流。
 *
 * <p>计数与设置过期必须在一起完成，否则「第一次 INCR 之后、EXPIRE 之前」进程挂掉，
 * 这个 key 就永远不过期，该用户会被永久限流。这里用一段 Lua 脚本把两步合成原子操作：
 *
 * <pre>
 * local current = redis.call('incr', KEYS[1])
 * if current == 1 then redis.call('expire', KEYS[1], ARGV[1]) end
 * return current
 * </pre>
 *
 * <p>为什么维度默认按「用户」而不是「IP」：本项目挂号接口的压测是 30 个账号从同一台
 * 机器发起的，按 IP 限流会直接把正常压测打成限流；按用户限流才是真正要防的「一个账号刷单」。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    /** 原子「自增 + 首次设置过期」，返回当前窗口内的请求次数 */
    private static final String COUNTER_SCRIPT = """
            local current = redis.call('incr', KEYS[1])
            if current == 1 then
                redis.call('expire', KEYS[1], ARGV[1])
            end
            return current
            """;

    private static final DefaultRedisScript<Long> COUNTER =
            new DefaultRedisScript<>(COUNTER_SCRIPT, Long.class);

    private static final String KEY_PREFIX = "rate:limit:";

    private final StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = buildKey(joinPoint, rateLimit);

        Long count;
        try {
            count = stringRedisTemplate.execute(
                    COUNTER, List.of(key), String.valueOf(rateLimit.windowSeconds()));
        } catch (Exception e) {
            // Redis 故障时放行：限流是保护措施而非业务规则，不该因为缓存抖动让接口整体不可用
            log.warn("限流计数失败，本次放行：{}", e.getMessage());
            return joinPoint.proceed();
        }

        if (count != null && count > rateLimit.limit()) {
            log.warn("触发限流：key={} 当前={} 上限={}/{}秒",
                    key, count, rateLimit.limit(), rateLimit.windowSeconds());
            throw new BusinessException(429, rateLimit.message());
        }

        return joinPoint.proceed();
    }

    private String buildKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String prefix = StringUtils.hasText(rateLimit.prefix())
                ? rateLimit.prefix()
                : defaultPrefix(joinPoint);

        if (rateLimit.byUser()) {
            Long userId = SecurityUtil.getCurrentUserIdOrNull();
            if (userId != null) {
                return KEY_PREFIX + prefix + ":user:" + userId;
            }
            // 理论上不会走到：需要登录的接口在进入切面前已被认证过滤器拦下
        }
        return KEY_PREFIX + prefix + ":ip:" + clientIp();
    }

    private String defaultPrefix(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

    private String clientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();

        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            // 经过 Nginx 等代理时，第一个才是真实客户端
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
