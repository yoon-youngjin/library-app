package com.group.libraryapp.repository.loanhistory

import com.group.libraryapp.domain.user.loadhistory.QUserLoanHistory.userLoanHistory
import com.group.libraryapp.domain.user.loadhistory.UserLoanHistory
import com.group.libraryapp.domain.user.loadhistory.UserLoanStatus
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Component

@Component
class UserLoanHistoryQuerydslRepository(
    private val queryFactory: JPAQueryFactory,
) {
    fun findBy(bookName: String, status: UserLoanStatus?): UserLoanHistory? {
        return queryFactory.select(userLoanHistory)
            .from(userLoanHistory)
            .where(
                userLoanHistory.bookName.eq(bookName),
                status?.let { userLoanHistory.status.eq(status) }
            )
            .limit(1)
            .fetchOne()
    }

    fun countBy(status: UserLoanStatus?): Long {
        return queryFactory.select(userLoanHistory.count())
            .from(userLoanHistory)
            .where(
                userLoanHistory.status.eq(status)
            )
            .fetchOne() ?: 0L
    }

}