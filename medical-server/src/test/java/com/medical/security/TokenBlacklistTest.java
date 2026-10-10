package com.medical.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link TokenBlacklist} 的单元测试，只关心两个设计属性：
 * 存的是摘要不是明文、TTL 与 token 剩余寿命对齐。
 */
@ExtendWith(MockitoExtension.class)
class TokenBlacklistTest {

    private static final long ONE_HOUR_MS = 3_600_000L;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private JwtUtil jwtUtil;
    private TokenBlacklist tokenBlacklist;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "unit-test-secret");
        ReflectionTestUtils.setField(jwtUtil, "expiration", ONE_HOUR_MS);
        tokenBlacklist = new TokenBlacklist(stringRedisTemplate, jwtUtil);
    }

    @Test
    @DisplayName("拉黑时写进 Redis 的是 SHA-256 摘要，不是 token 明文")
    void revokedTokenIsStoredAsDigest() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        String token = jwtUtil.generateToken(7L, "patient001", 1);

        assertThat(tokenBlacklist.revoke(token)).isTrue();

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), eq("1"), anyLong(), eq(TimeUnit.MILLISECONDS));

        String key = keyCaptor.getValue();
        // 缓存内容万一泄漏，光有摘要没法直接拿去调接口
        assertThat(key).doesNotContain(token);
        assertThat(key).isEqualTo("auth:blacklist:" + sha256(token));
    }

    @Test
    @DisplayName("黑名单条目的 TTL 对齐 token 剩余有效期，不会无限膨胀")
    void ttlMatchesRemainingLifetime() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        String token = jwtUtil.generateToken(7L, "patient001", 1);

        tokenBlacklist.revoke(token);

        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(valueOperations).set(anyString(), eq("1"), ttlCaptor.capture(), eq(TimeUnit.MILLISECONDS));

        // token 总有效期 1 小时，拉黑的那一刻剩余不到 1 小时；留 10 秒余量避免执行抖动
        assertThat(ttlCaptor.getValue()).isBetween(ONE_HOUR_MS - 10_000, ONE_HOUR_MS);
    }

    @Test
    @DisplayName("已经过期的 token 不需要拉黑，也不该占用 Redis")
    void expiredTokenIsNotRevoked() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1_000L);
        String expired = jwtUtil.generateToken(7L, "patient001", 1);

        assertThat(tokenBlacklist.revoke(expired)).isFalse();
        // 已经不可能通过认证的 token，写进黑名单是纯粹的空间浪费
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("不是 token 的字符串不拉黑，也不能把异常抛出去")
    void malformedTokenIsNotRevoked() {
        assertThat(tokenBlacklist.revoke("garbage")).isFalse();
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("isRevoked 忠实反映 Redis 里的状态")
    void isRevokedReflectsRedisState() {
        String token = jwtUtil.generateToken(7L, "patient001", 1);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true, false);

        assertThat(tokenBlacklist.isRevoked(token)).isTrue();
        assertThat(tokenBlacklist.isRevoked(token)).isFalse();
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
