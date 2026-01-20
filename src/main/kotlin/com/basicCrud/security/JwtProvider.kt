package com.basicCrud.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtProvider {
    private val secretString = "your-very-secure-and-long-secret-key-32-chars-at-least"
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(secretString.toByteArray())
    private val expirationTime = 3600000L // 1시간

    // 토큰 생성
    fun createToken(loginId: String): String {
        val now = Date()
        return Jwts.builder()
            .subject(loginId)
            .issuedAt(now)
            .expiration(Date(now.time + expirationTime))
            .signWith(secretKey)
            .compact()
    }

    // 아이디 추출
    fun getId(token: String): String {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
            .subject
    }

    // 유효성 검사
    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }
}