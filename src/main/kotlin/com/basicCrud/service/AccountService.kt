package com.basicCrud.service

import com.basicCrud.controller.dto.SignupRequest
import com.basicCrud.domain.Account
import com.basicCrud.repository.AccountRepository
import com.basicCrud.security.JwtProvider
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val jwtProvider: JwtProvider

) {
    // 메시지 설정
    companion object {
        const val DUPLICATE_ID_MESSAGE = "이미 존재하는 아이디입니다."
        const val DIFFERENT_ID_OR_PW_MESSAGE = "아이디가 존재하지 않거나 비밀번호가 일치하지 않습니다."
        const val ID_DOES_NOT_EXIST_MESSAGE = "존재하지 않는 아이디입니다."
    }

    // 회원가입
    @Transactional
    fun signUp(request: SignupRequest): Account {
        // 1. 중복 아이디 체크
        if (accountRepository.findById(request.id) != null) {
            // 2-(1) 이미 존재하는 아이디일 경우 오류 메시지 반환
            throw IllegalArgumentException(DUPLICATE_ID_MESSAGE)
        }

        // 2-(2) 아이디 중복이 아닌 경우 비밀번호 암호화 및 저장
        val newAccount = Account(
            id = request.id,
            password = passwordEncoder.encode(request.password),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        return accountRepository.save(newAccount)
    }

    // 로그인
    @Transactional(readOnly = true)
    fun login(id: String, pw: String): String {
        return accountRepository.findById(id)
            // 1. 아이디 존재 유무 확인 후 비밀번호 일치 여부 확인
            //    비밀번호가 일치하는 경우 account 객체를 유지, 아니면 null 반환
            ?.takeIf { account -> passwordEncoder.matches(pw, account.password) }

            // 2-(1) 로그인 성공시 JWT 토큰 생성
            ?.let { it.id?.let { it1 -> jwtProvider.createToken(it1) } }

            // 2-(2) 로그인 실패시 오류 메시지 반환 (아이디가 존재하지 않거나 비밀번호가 일치하지 않는 경우)
            ?: throw IllegalArgumentException(DIFFERENT_ID_OR_PW_MESSAGE)
    }

    // 정보 수정
    @Transactional
    fun updateAccount(id: String, newPassword: String) {
        // 1. 사용자 ID 존재 여부 확인
        val account = accountRepository.findById(id)

        // 2-(1) 사용자 ID가 존재하지 않는 경우 오류 메시지 반환
            ?: throw IllegalArgumentException(ID_DOES_NOT_EXIST_MESSAGE)

        // 2-(2) 사용자 ID가 존재하는 경우 새로운 비밀번호 암호화 후 업데이트
        val encodedPassword = passwordEncoder.encode(newPassword)
        account.updatePassword(encodedPassword)
    }

    // 회원 탈퇴
    @Transactional
    fun deleteAccount(id: String) {
        // 1. 사용자 ID 존재 여부 확인
        val account = accountRepository.findById(id)

        // 2-(1) 사용자 ID가 존재하지 않는 경우 오류 메시지 반환
            ?: throw IllegalArgumentException(ID_DOES_NOT_EXIST_MESSAGE)

        // 2-(2) 사용자 ID가 존재하는 경우 데이터 삭제
        accountRepository.delete(account)
    }

}