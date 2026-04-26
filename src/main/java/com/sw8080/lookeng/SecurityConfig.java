package com.sw8080.lookeng;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // REST API 기반이므로 CSRF는 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                // 브라우저 팝업창 로그인 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)
                // 기본 로그인 폼 비활성화 (우리가 직접 만든 API를 쓸 것임)
                .formLogin(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        // 회원가입, 로그인은 누구나 접근 가능
                        .requestMatchers("/api/v1/auth/signup", "/api/v1/auth/login").permitAll()
                        // 그 외 API는 인증(세션) 필요 (개발 편의를 위해 당분간 permitAll()로 두셔도 됩니다)
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
