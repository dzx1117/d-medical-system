package com.medical.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.common.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 只有登录/注册/退出是对外开放的；/auth/info 需要登录，
                // 放在 anyRequest().authenticated() 里才能返回真正的 HTTP 401
                .requestMatchers("/auth/login", "/auth/register", "/auth/logout",
                        "/health", "/doc.html", "/webjars/**",
                        "/swagger-ui/**", "/v3/api-docs/**", "/favicon.ico").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 管理员专属：科室、医生、排班、药品的增删改
                .requestMatchers(HttpMethod.POST, "/department/**", "/doctor/**",
                        "/schedule/**", "/medicine/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/department/**", "/doctor/**",
                        "/schedule/**", "/medicine/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/department/**", "/doctor/**",
                        "/schedule/**", "/medicine/**").hasRole("ADMIN")
                // 统计看板仅管理员
                .requestMatchers("/statistics/**").hasRole("ADMIN")
                // 医生专属
                .requestMatchers("/appointment/doctor", "/appointment/complete/**",
                        "/record/doctor").hasRole("DOCTOR")
                .requestMatchers(HttpMethod.POST, "/record", "/prescription").hasRole("DOCTOR")
                .requestMatchers(HttpMethod.PUT, "/record").hasRole("DOCTOR")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> writeJson(res, 401, "未登录或登录已过期"))
                .accessDeniedHandler((req, res, e) -> writeJson(res, 403, "没有权限访问该资源"))
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeJson(HttpServletResponse response, int code, String message) throws java.io.IOException {
        response.setStatus(code);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, message)));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
