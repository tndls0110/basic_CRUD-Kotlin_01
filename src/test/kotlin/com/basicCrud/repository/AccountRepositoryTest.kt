package com.basicCrud.repository

import com.basicCrud.domain.Account
import java.time.LocalDateTime

// 이하 Gemini 3.0으로 작성 및 수정
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

@DataJpaTest // JPA 관련 설정만 테스트
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 실제 DB(PostgreSQL)에 테스트
class AccountRepositoryTest @Autowired constructor(
    private val accountRepository: AccountRepository
) {

    @Test
    @DisplayName("회원 정보 저장하기")
    fun saveAccountTest() {
        // 1. Given: 저장할 계정 객체 생성
        val account = Account(
            id = "testuser",
            password = "password123",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        // 2. When: DB에 저장
        val savedAccount = accountRepository.save(account)

        // 3. Then: 저장된 값 검증
        assertThat(savedAccount.seq).isNotNull() // seq가 IDENTITY 전략으로 생성되었는지 확인
        assertThat(savedAccount.id).isEqualTo("testuser")
        assertThat(savedAccount.createdAt).isNotNull() // JpaAuditing 작동 확인
        assertThat(savedAccount.updatedAt).isNotNull()
    }

}