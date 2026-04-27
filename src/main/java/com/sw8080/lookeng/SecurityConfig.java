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
                        // auth와 error, 그리고 단어(words) API까지 스프링 시큐리티는 무사통과!
                        .requestMatchers("/api/v1/auth/**",
                                "/error",
                                "/api/v1/words/**",
                                "/api/v1/test/**").permitAll()

                        // (만약 앞으로 귀찮다면 아예 .anyRequest().permitAll() 로 다 열어두고,
                        // 지금처럼 컨트롤러에서 세션을 검사하는 방식을 유지하셔도 됩니다!)
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}
