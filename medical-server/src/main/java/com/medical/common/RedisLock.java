package com.medical.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基于 Redis 的分布式锁。
 *
 * <p>加锁使用 {@code SET key value NX PX ttl}，保证「判断 + 设置」的原子性；
 * 解锁使用 Lua 脚本比对持有者标识后再删除，避免误删他人持有的锁。
 * 同时提供带自旋的重试机制，应对瞬时竞争。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLock {

    /** 解锁 Lua 脚本：仅当 value 与自身持有的一致时才删除，保证原子性 */
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

    private static final DefaultRedisScript<Long> UNLOCK = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试加锁一次，不重试。
     *
     * @param key      锁的键
     * @param value    持有者标识，解锁时校验
     * @param expireMs 锁自动过期时间（毫秒），防止宕机后死锁
     * @return 是否加锁成功
     */
    public boolean tryLock(String key, String value, long expireMs) {
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, value, expireMs, TimeUnit.MILLISECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 解锁：比对持有者标识，一致才删除。
     */
    public boolean unlock(String key, String value) {
        Long result = stringRedisTemplate.execute(UNLOCK, Collections.singletonList(key), value);
        return result != null && result > 0;
    }

    /**
     * 带自旋重试的加锁执行模板。
     *
     * @param key             锁键
     * @param waitMs          最大等待时间（毫秒）
     * @param expireMs        锁自动过期时间（毫秒）
     * @param retryIntervalMs 重试间隔（毫秒）
     * @param action          加锁成功后执行的业务逻辑
     * @return 业务返回值
     */
    public <T> T executeWithLock(String key, long waitMs, long expireMs, long retryIntervalMs, Supplier<T> action) {
        String value = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + waitMs;

        while (System.currentTimeMillis() < deadline) {
            if (tryLock(key, value, expireMs)) {
                try {
                    return action.get();
                } finally {
                    unlock(key, value);
                }
            }
            try {
                Thread.sleep(retryIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException("获取锁被中断");
            }
        }
        throw new BusinessException("当前挂号人数较多，请稍后重试");
    }
}
