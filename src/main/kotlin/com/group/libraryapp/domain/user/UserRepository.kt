package com.group.libraryapp.domain.user

import java.util.Optional
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {

    fun findByName(name: String): User? // 쿼리 실행 결과가 존재하지 않으면 자동으로 null을 넣어준다.
}