package com.group.libraryapp.service.book

import com.group.libraryapp.domain.book.Book
import com.group.libraryapp.domain.book.BookRepository
import com.group.libraryapp.domain.book.BookType
import com.group.libraryapp.domain.user.User
import com.group.libraryapp.domain.user.UserRepository
import com.group.libraryapp.domain.user.loadhistory.UserLoanHistory
import com.group.libraryapp.domain.user.loadhistory.UserLoanHistoryRepository
import com.group.libraryapp.domain.user.loadhistory.UserLoanStatus
import com.group.libraryapp.dto.book.request.BookLoanRequest
import com.group.libraryapp.dto.book.request.BookRequest
import com.group.libraryapp.dto.book.request.BookReturnRequest
import com.group.libraryapp.dto.book.response.BookStatResponse
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class BookServiceTest @Autowired constructor(
    private val bookService: BookService,
    private val bookRepository: BookRepository,
    private val userLoanHistoryRepository: UserLoanHistoryRepository,
    private val userRepository: UserRepository,
) {

    @AfterEach
    fun clean() {
        bookRepository.deleteAllInBatch()
        userLoanHistoryRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
    }

    @Test
    @DisplayName("책 등록이 정상 동작한다")
    fun saveBookTest() {
        // given
        val request = BookRequest("test", BookType.COMPUTER)

        // when
        bookService.saveBook(request)

        // then
        val results = bookRepository.findAll()
        assertSoftly(results) {
            it.size shouldBe 1
            it[0].name shouldBe "test"
            it[0].type shouldBe BookType.COMPUTER
        }
    }

    @Test
    @DisplayName("책 대출이 정상 동작한다")
    fun loanBookTest() {
        // given
        val targetBookName = "target"
        val targetName = "유녕진"
        bookRepository.save(Book.fixture(targetBookName))
        val savedUser = userRepository.save(
            User(
                targetName,
                null
            )
        )
        val request = BookLoanRequest(targetName, targetBookName)

        // when
        bookService.loanBook(request)

        // then
        val results = userLoanHistoryRepository.findAll()
        assertSoftly(results) {
            it.size shouldBe 1
            it[0].bookName shouldBe targetBookName
            it[0].user.id shouldBe savedUser.id
            it[0].user.name shouldBe targetName
            it[0].status shouldBe UserLoanStatus.LOANED

        }
    }

    @Test
    @DisplayName("책이 이미 대출되어 있다면, 신규 대출이 실패한다.")
    fun loanBookFailTest() {
        // given
        bookRepository.save(Book.fixture("test"))
        val savedUser = userRepository.save(User("유녕진", null))
        val request = BookLoanRequest("유녕진", "test")
        userLoanHistoryRepository.save(UserLoanHistory.fixture(savedUser, "test"))

        // when then
        shouldThrow<IllegalArgumentException> {
            bookService.loanBook(request)
        }.message shouldBe "진작 대출되어 있는 책입니다"
    }

    @Test
    @DisplayName("책 반납이 정상 동작한다")
    fun returnBookTest() {
        bookRepository.save(Book.fixture("test"))
        val savedUser = userRepository.save(User("유녕진", null))
        val request = BookReturnRequest(savedUser.name, "test")
        userLoanHistoryRepository.save(UserLoanHistory.fixture(savedUser, "test"))

        // when
        bookService.returnBook(request)

        // then
        val results = userLoanHistoryRepository.findAll()
        assertSoftly(results) {
            it[0].status shouldBe UserLoanStatus.RETURNED
        }
    }

    @Test
    @DisplayName("책 대여 권수를 정상 확인한다")
    fun countLoanedBookTest() {
        // given
        val savedUser = userRepository.save(User("유녕진", null))
        userLoanHistoryRepository.saveAll(
            listOf(
                UserLoanHistory.fixture(savedUser, "A"),
                UserLoanHistory.fixture(savedUser, "B", UserLoanStatus.RETURNED),
                UserLoanHistory.fixture(savedUser, "C", UserLoanStatus.RETURNED)
            )
        )

        // when
        val result = bookService.countLoanedBook()

        // then
        assertSoftly(result) {
            it shouldBe 2
        }
    }

    @Test
    @DisplayName("분야별 책 권수를 정상 확인한다")
    fun getBookStatisticsTest() {
        // given
        bookRepository.saveAll(
            listOf(
                Book.fixture("A", BookType.COMPUTER),
                Book.fixture("B", BookType.COMPUTER),
                Book.fixture("C", BookType.SCIENCE),
            )
        )

        // when
        val result = bookService.getBookStatistics()

        // then
//        assertThat(result).hasSize(2)
//        assertCount(result, BookType.COMPUTER, 2L)
//        assertCount(result, BookType.SCIENCE, 1L)
        assertSoftly(result) { it ->
            it.size shouldBe 2
            it.first { it.type == BookType.COMPUTER }.count shouldBe 2
            it.first { it.type == BookType.SCIENCE }.count shouldBe 1
        }
//        val computerDto = result.first { result -> result.type == BookType.COMPUTER}
//        assertThat(computerDto.count).isEqualTo(2)
//
//        val scienceDto = result.first { result -> result.type == BookType.SCIENCE}
//        assertThat(scienceDto.count).isEqualTo(1)
    }


    private fun assertCount(results: List<BookStatResponse>, type: BookType, count: Long) {
        assertThat(results.first { result -> result.type == type }.count).isEqualTo(count)
    }

}