package com.medical.common;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RateLimitAspect} 的单元测试。
 *
 * <p>最要紧的一条是「Redis 挂了要放行」：限流是保护措施，不是业务规则。
 * 如果这里改成抛异常，Redis 一抖动整个接口就全挂 —— 保护措施反而成了单点故障。
 */
@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ProceedingJoinPoint joinPoint;

    private RateLimitAspect rateLimitAspect;

    @BeforeEach
    void setUp() {
        rateLimitAspect = new RateLimitAspect(stringRedisTemplate);
    }

    @Test
    @DisplayName("窗口内没超限时，请求正常放行")
    void requestIsPassedThroughWhenUnderLimit() throws Throwable {
        stubCounter(1L);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = rateLimitAspect.around(joinPoint, rule(3));

        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("刚好达到上限仍然放行，超过才拦")
    void limitIsInclusiveBoundary() throws Throwable {
        stubCounter(3L);
        when(joinPoint.proceed()).thenReturn("ok");

        // limit=3 表示「3 次以内允许」，第 4 次才拦 —— 边界差一位是最常见的改坏方式
        assertThat(rateLimitAspect.around(joinPoint, rule(3))).isEqualTo("ok");
    }

    @Test
    @DisplayName("超过上限抛 429，并带上注解里配的提示语")
    void requestIsRejectedWhenOverLimit() throws Throwable {
        stubCounter(4L);

        assertThatThrownBy(() -> rateLimitAspect.around(joinPoint, rule(3)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("操作过于频繁")
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(429);
    }

    @Test
    @DisplayName("Redis 故障时放行请求，而不是让接口整体不可用")
    void redisFailureIsFailOpen() throws Throwable {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenThrow(new RuntimeException("redis 连接超时"));
        when(joinPoint.proceed()).thenReturn("ok");

        // 限流是保护措施而非业务规则，缓存抖动不该演变成业务中断
        assertThat(rateLimitAspect.around(joinPoint, rule(3))).isEqualTo("ok");
    }

    @Test
    @DisplayName("显式配了 prefix 时，限流 key 用它而不是方法全名")
    void explicitPrefixIsUsedInKey() throws Throwable {
        stubCounter(1L);
        when(joinPoint.proceed()).thenReturn("ok");

        rateLimitAspect.around(joinPoint, rule(3));

        // 未登录时退化成 IP 维度；测试环境没有请求上下文，落到 unknown
        verify(stringRedisTemplate)
                .execute(any(RedisScript.class), eq(List.of("rate:limit:register:ip:unknown")), anyString());
    }

    @Test
    @DisplayName("没配 prefix 时，key 退化成「类名.方法名」")
    void defaultPrefixFallsBackToMethodName() throws Throwable {
        stubCounter(1L);
        when(joinPoint.proceed()).thenReturn("ok");

        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getDeclaringType()).thenReturn(PrescriptionServiceImplStub.class);
        when(signature.getName()).thenReturn("pay");
        when(joinPoint.getSignature()).thenReturn(signature);

        rateLimitAspect.around(joinPoint, ruleWithoutPrefix(3));

        verify(stringRedisTemplate).execute(any(RedisScript.class),
                eq(List.of("rate:limit:PrescriptionServiceImplStub.pay:ip:unknown")), anyString());
    }

    private void stubCounter(Long count) {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(count);
    }

    private RateLimit rule(int limit) {
        return rule(limit, "register");
    }

    private RateLimit rule(int limit, String prefix) {
        RateLimit rateLimit = mock(RateLimit.class);
        when(rateLimit.prefix()).thenReturn(prefix);
        when(rateLimit.windowSeconds()).thenReturn(60);
        when(rateLimit.byUser()).thenReturn(false);
        // limit 只在 Redis 计数成功返回后才会被读到，message 只在超限抛异常时才会被读到。
        // 放行用例和 Redis 故障用例都走不到这两条，所以是 lenient。
        lenient().when(rateLimit.limit()).thenReturn(limit);
        lenient().when(rateLimit.message()).thenReturn("操作过于频繁，请稍后再试");
        return rateLimit;
    }

    private RateLimit ruleWithoutPrefix(int limit) {
        RateLimit rateLimit = mock(RateLimit.class);
        when(rateLimit.prefix()).thenReturn("");
        when(rateLimit.windowSeconds()).thenReturn(60);
        when(rateLimit.byUser()).thenReturn(false);
        lenient().when(rateLimit.limit()).thenReturn(limit);
        lenient().when(rateLimit.message()).thenReturn("操作过于频繁，请稍后再试");
        return rateLimit;
    }

    /** 只为了让 {@code getDeclaringType().getSimpleName()} 有个稳定的名字可用 */
    static class PrescriptionServiceImplStub {
    }
}
