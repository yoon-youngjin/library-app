package com.group.libraryapp.domain.user.loadhistory

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserLoanHistoryRepository : JpaRepository<UserLoanHistory, Long> {

//    fun findByBookName(bookName: String): UserLoanHistory

//    fun findByBookNameAndStatus(bookName: String, status: UserLoanStatus): UserLoanHistory?

//    fun findAllByStatus(status: UserLoanStatus): List<UserLoanHistory>

//    fun countByStatus(status: UserLoanStatus): Long

}