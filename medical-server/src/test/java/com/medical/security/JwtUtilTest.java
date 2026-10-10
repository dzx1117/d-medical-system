package com.medical.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JwtUtil} 的单元测试。
 *
 * <p>这个类不依赖 Spring 容器，secret / expiration 通过反射注入，跑起来很快。
 */
class JwtUtilTest {

    private static final String SECRET = "unit-test-secret";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
    }

    @Test
    @DisplayName("签发的 token 能通过校验，并带出正确的用户信息")
    void generatedTokenCarriesUserInfo() {
        String token = jwtUtil.generateToken(42L, "patient001", 1);

        DecodedJWT decoded = jwtUtil.verifyToken(token);

        assertThat(decoded.getSubject()).isEqualTo("42");
        assertThat(decoded.getClaim("username").asString()).isEqualTo("patient001");
        assertThat(decoded.getClaim("userType").asInt()).isEqualTo(1);
        assertThat(jwtUtil.getUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("同一秒内连续签发 20 次，得到 20 个互不相同的 token")
    void tokensIssuedWithinTheSameSecondAreUnique() {
        // 这是那个真实缺陷的回归测试：
        // JWT 载荷里只有秒级的 iat，没有 jti 的话，同一秒内两次登录会签出逐字节相同的字符串。
        // 黑名单按 token 的 SHA-256 摘要存 key，于是重新登录拿到的「新」token 一出生就被
        // 判定为已拉黑 —— 表现为「退出登录后马上重新登录，新 token 一用就 401」。
        // 循环里不加任何 sleep，全部落在同一秒内，正是当时的触发条件。
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            tokens.add(jwtUtil.generateToken(1L, "patient001", 1));
        }

        assertThat(tokens).hasSize(20);
    }

    @Test
    @DisplayName("改动过签名的 token 校验不通过")
    void tokenWithTamperedSignatureIsRejected() {
        String token = jwtUtil.generateToken(1L, "patient001", 1);
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("换一把密钥签发的 token 不被接受")
    void tokenSignedWithAnotherSecretIsRejected() {
        String foreign = JWT.create()
                .withSubject("1")
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 60_000))
                .sign(Algorithm.HMAC256("another-secret"));

        assertThat(jwtUtil.validateToken(foreign)).isFalse();
    }

    @Test
    @DisplayName("已过期的 token 校验不通过")
    void expiredTokenIsRejected() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1_000L);
        String expired = jwtUtil.generateToken(1L, "patient001", 1);

        assertThat(jwtUtil.validateToken(expired)).isFalse();
    }

    @Test
    @DisplayName("根本不是 token 的字符串，应该返回 false 而不是抛异常")
    void malformedTokenIsRejectedInsteadOfThrowing() {
        // 认证过滤器会把用户传来的任意字符串喂给这里。如果不返回 false 而是抛出去，
        // 一个畸形 token 就会让接口返回 500 —— 本该是 401。
        assertThat(jwtUtil.validateToken("not-a-jwt-at-all")).isFalse();
        assertThat(jwtUtil.validateToken("a.b.c")).isFalse();
        assertThat(jwtUtil.validateToken("")).isFalse();
    }
}
