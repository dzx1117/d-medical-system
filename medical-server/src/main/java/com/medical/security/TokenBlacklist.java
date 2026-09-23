package com.medical.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/**
 * JWT 黑名单（退出登录后让 token 立即失效）。
 *
 * <p>JWT 是无状态的：签发后服务端不保存任何记录，所以「退出登录」如果只是前端删掉
 * localStorage 里的 token，那个 token 在过期前依然可以继续调用接口。这里用 Redis
 * 存一份黑名单来补上这个缺口。
 *
 * <p>两个关键设计：
 * <ul>
 *   <li>只存 token 的 <b>SHA-256 摘要</b>，不存明文 token，避免缓存泄漏后被直接拿去调用接口；</li>
 *   <li>黑名单条目的 TTL 设为该 token 的 <b>剩余有效期</b>，token 自然过期后条目也会被
 *       Redis 自动清理，黑名单不会无限膨胀。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlacklist {

    private static final String KEY_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtil jwtUtil;

    /**
     * 把 token 拉黑，有效期与 token 的剩余寿命对齐。
     *
     * @return 是否成功拉黑（token 本身已过期或无法解析时返回 false，无需拉黑）
     */
    public boolean revoke(String token) {
        DecodedJWT jwt;
        try {
            jwt = jwtUtil.verifyToken(token);
        } catch (Exception e) {
            // 已经过期/非法的 token 不可能再通过认证，不必占用黑名单
            log.debug("退出登录时 token 已失效，跳过拉黑");
            return false;
        }

        long remainMs = jwt.getExpiresAt().getTime() - Instant.now().toEpochMilli();
        if (remainMs <= 0) {
            return false;
        }

        stringRedisTemplate.opsForValue()
                .set(keyOf(token), "1", remainMs, TimeUnit.MILLISECONDS);
        log.info("用户 {} 退出登录，token 已加入黑名单，剩余有效期 {} 秒",
                jwt.getSubject(), remainMs / 1000);
        return true;
    }

    /**
     * token 是否已被拉黑。认证过滤器在解析 token 后调用。
     */
    public boolean isRevoked(String token) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(keyOf(token)));
    }

    private String keyOf(String token) {
        return KEY_PREFIX + sha256(token);
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 必备算法，正常不会走到这里
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", e);
        }
    }
}
