package com.group.libraryapp.domain.user.loadhistory

import com.group.libraryapp.domain.user.User
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne

@Entity
class UserLoadHistory(

    @ManyToOne
    val user: User,

    val bookName: String,

    var status: UserLoanStatus = UserLoanStatus.LOANED,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {

    fun doReturn() {
        this.status = UserLoanStatus.RETURNED
    }

    companion object {
        fun fixture(
            user: User,
            bookName: String = "test",
            status: UserLoanStatus = UserLoanStatus.LOANED,
            id: Long? = null
        ): UserLoadHistory {
            return UserLoadHistory(
                user = user,
                bookName = bookName,
                status = status,
                id = id
            )
        }
    }

}