package com.medical.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

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
