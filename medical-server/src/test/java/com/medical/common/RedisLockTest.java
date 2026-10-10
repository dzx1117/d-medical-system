package com.medical.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RedisLock} 的单元测试。
 *
 * <p>注意这里测不出 Lua 脚本本身的语义 —— 解锁「比对持有者再删」的原子性靠的是
 * Redis 执行脚本，Mock 只能验证我们确实发起了调用。真正的并发正确性由
 * {@code tests/e2e-test-concurrency.sh} 在真实 Redis 上验证。
 * 这里盯的是两条容易被改坏的逻辑：返回值映射，和异常路径下锁有没有被释放。
 */
@ExtendWith(MockitoExtension.class)
class RedisLockTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisLock redisLock;

    @BeforeEach
    void setUp() {
        redisLock = new RedisLock(stringRedisTemplate);
    }

    @Test
    @DisplayName("tryLock 把 setIfAbsent 的结果映射成布尔值")
    void tryLockMapsSetIfAbsentResult() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("lock:schedule:1"), anyString(), eq(10_000L),
                eq(TimeUnit.MILLISECONDS))).thenReturn(true, null);

        assertThat(redisLock.tryLock("lock:schedule:1", "holder-a", 10_000)).isTrue();
        // 底层客户端返回 null 时不能被当成加锁成功
        assertThat(redisLock.tryLock("lock:schedule:1", "holder-b", 10_000)).isFalse();
    }

    @Test
    @DisplayName("业务抛异常时锁依然会被释放")
    void lockIsReleasedWhenActionThrows() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(1L);

        assertThatThrownBy(() -> redisLock.executeWithLock("lock:schedule:1", 100, 10_000, 10, () -> {
            throw new IllegalStateException("业务炸了");
        })).isInstanceOf(IllegalStateException.class);

        // 解锁不放在 finally 里的话，业务一异常锁就留到 TTL 超时，这期间所有人都挂不上号
        verify(stringRedisTemplate).execute(any(RedisScript.class), anyList(), anyString());
    }

    @Test
    @DisplayName("抢不到锁时按间隔重试，超过等待时间后抛业务异常，且业务一次都没执行")
    void throwsAfterWaitingTooLong() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(false);

        AtomicInteger executions = new AtomicInteger();

        assertThatThrownBy(() -> redisLock.executeWithLock("lock:schedule:1", 80, 10_000, 10, () -> {
            executions.incrementAndGet();
            return "不会走到这里";
        }))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("挂号人数较多");

        assertThat(executions).hasValue(0);
        // 等待窗口内应该反复重试，而不是试一次就放弃
        verify(valueOperations, atLeast(3))
                .setIfAbsent(anyString(), anyString(), anyLong(), any());
    }
}
