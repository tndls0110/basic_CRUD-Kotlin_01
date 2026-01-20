package com.basicCrud.controller

import com.basicCrud.controller.dto.SignupRequest
import com.basicCrud.controller.dto.UpdateRequest
import com.basicCrud.security.JwtProvider
import com.basicCrud.service.AccountService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/accounts")
class AccountController(
    private val accountService: AccountService,
    private val jwtProvider: JwtProvider
) {
    // 회원가입
    @PostMapping("/signup")
    fun signup(@RequestBody request: SignupRequest): ResponseEntity<Any> {
        return try {
            accountService.signUp(request)
            ResponseEntity.status(HttpStatus.CREATED).build() // 아이디 중복이 아닌 경우 201(Created) 응답
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message)) // 아이디 중복인 경우 에러 메시지와 함께 400 반환
        }
    }

    // 로그인
    @GetMapping("/login")
    fun login(
        @RequestParam id: String,
        @RequestParam(name = "password") pw: String
    ): ResponseEntity<Map<String, String>> {
        return try {
            val token = accountService.login(id, pw)
            ResponseEntity.ok(mapOf("accessToken" to token)) // 로그인 성공시 토큰 반환
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build() // 로그인 실패 시 401 또는 400 처리
        }
    }

    // 정보 수정
    @PatchMapping("/me")
    fun update(
        @RequestHeader("Authorization", required = false) authHeader: String?, // "Bearer ..." 형태
        @RequestBody request: UpdateRequest
    ): ResponseEntity<Unit> {
        if (authHeader == null) { // 헤더가 없을 경우 403 반환
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val token = authHeader.substringAfter("Bearer ") // 토큰에서 순수 값만 추출한 뒤 (Bearer 제거)

        if (!jwtProvider.validateToken(token)) { // 토큰 유효성 검사
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val id = jwtProvider.getId(token)
        accountService.updateAccount(id, request.newPassword)

        return ResponseEntity.noContent().build() // 204 No Content 반환
    }

    // 회원 탈퇴
    @DeleteMapping("/me")
    fun withdraw(
        @RequestHeader("Authorization", required = false) authHeader: String? // "Bearer ..." 형태
    ): ResponseEntity<Unit> {
        if (authHeader == null) { // 헤더가 없을 경우 403 반환
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val token = authHeader.substringAfter("Bearer ") // 토큰에서 순수 값만 추출한 뒤 (Bearer 제거)

        if (!jwtProvider.validateToken(token)) { // 토큰 유효성 검사
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val id = jwtProvider.getId(token)
        accountService.deleteAccount(id)

        return ResponseEntity.noContent().build() // 204 No Content 반환
    }
}