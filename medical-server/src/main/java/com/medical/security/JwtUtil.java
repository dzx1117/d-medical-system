package com.medical.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${jwt.secret:medical-system-secret-key}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    public String generateToken(Long userId, String username, Integer userType) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expiration);
        return JWT.create()
                .withSubject(String.valueOf(userId))
                // jti 保证每次签发的 token 都是唯一的。没有它的话，载荷里只有秒级的 iat，
                // 同一秒内两次登录会签出完全相同的字符串，黑名单按摘要存 key 就会误伤新 token
                // ——表现为「退出后立刻重新登录，拿到的新 token 一用就 401」。
                .withJWTId(UUID.randomUUID().toString())
                .withClaim("username", username)
                .withClaim("userType", userType)
                .withIssuedAt(now)
                .withExpiresAt(expireDate)
                .sign(Algorithm.HMAC256(secret));
    }

    public DecodedJWT verifyToken(String token) {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret)).build();
        return verifier.verify(token);
    }

    public Long getUserId(String token) {
        DecodedJWT jwt = verifyToken(token);
        return Long.valueOf(jwt.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            verifyToken(token);
            return true;
        } catch (JWTVerificationException e) {
            return false;
        }
    }
}
