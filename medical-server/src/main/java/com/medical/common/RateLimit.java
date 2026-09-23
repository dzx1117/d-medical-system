package com.medical.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解，配合 {@link RateLimitAspect} 使用。
 *
 * <p>典型用法：给挂号、取消预约这类「写接口」加上，防止单个用户脚本刷单
 * 或误触连点造成的无效请求把号源锁争抢放大。
 *
 * <pre>{@code
 * @RateLimit(prefix = "appointment:create", limit = 5, windowSeconds = 10)
 * @PostMapping("/create")
 * public Result<AppointmentVO> create(@RequestParam Long scheduleId) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 限流键前缀，留空则默认取「类名.方法名」 */
    String prefix() default "";

    /** 时间窗口内允许的最大请求次数 */
    int limit() default 10;

    /** 时间窗口长度（秒） */
    int windowSeconds() default 60;

    /**
     * 限流维度。
     * true（默认）按登录用户限流——同一个账号在多台设备上也共享额度；
     * false 按客户端 IP 限流，用于登录这类「还没有用户身份」的接口。
     */
    boolean byUser() default true;

    /** 触发限流时返回给前端的提示 */
    String message() default "操作过于频繁，请稍后再试";
}
