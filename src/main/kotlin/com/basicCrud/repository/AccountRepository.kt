package com.basicCrud.repository

import com.basicCrud.domain.Account
import org.springframework.data.jpa.repository.JpaRepository

interface AccountRepository: JpaRepository<Account, Long> {

    // 아이디 존재 여부 확인
    fun findById(id: String): Account?

}