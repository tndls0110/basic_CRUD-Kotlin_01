package com.basicCrud.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "account")
open class Account (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seq", nullable = false)
    open var seq: Long? = null,

    @Column(name = "id", nullable = false, unique = true)
    open var id: String? = null,

    @Column(name = "password", nullable = false)
    open var password: String? = null,

    @Column(name = "created_at", nullable = false)
    open var createdAt: LocalDateTime? = null,

    @Column(name = "updated_at", nullable = false)
    open var updatedAt: LocalDateTime? = null
) {
    // 정보 수정
    fun updatePassword(encodedPassword: String) {
        this.password = encodedPassword
    }
}