package com.basicCrud.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() } // API 서버이므로 CSRF 비활성화
            .authorizeHttpRequests {
                it.requestMatchers("/api/v1/accounts/signup").permitAll()
                it.requestMatchers("/api/v1/accounts/login").permitAll()
                it.anyRequest().authenticated()
            }
        return http.build()
    }
}