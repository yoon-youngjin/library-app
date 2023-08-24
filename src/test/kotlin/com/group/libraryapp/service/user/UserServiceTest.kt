package com.group.libraryapp.service.user

import com.group.libraryapp.domain.user.User
import com.group.libraryapp.domain.user.UserRepository
import com.group.libraryapp.domain.user.loadhistory.UserLoanHistory
import com.group.libraryapp.domain.user.loadhistory.UserLoanHistoryRepository
import com.group.libraryapp.domain.user.loadhistory.UserLoanStatus
import com.group.libraryapp.dto.user.request.UserCreateRequest
import com.group.libraryapp.dto.user.request.UserUpdateRequest
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.containExactlyInAnyOrder
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
// 명시적으로 constructor 키워드를 붙이고 그 앞에 @Autowired를 붙이면 각 프로퍼티에 붙이는 @Autowired를 생략할 수 있다.
class UserServiceTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val userLoanHistoryRepository: UserLoanHistoryRepository,
) {
    @AfterEach
    fun clean() {
        userLoanHistoryRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
    }

    @Test
    fun saveUserTest() {
        // given
        val request = UserCreateRequest("유녕진", null)

        // when
        userService.saveUser(request)

        // then
        val results = userRepository.findAll()
        assertSoftly(results) {
            it.size shouldBe 1
            it[0].name shouldBe "유녕진"
            it[0].age shouldBe null
        }
    }

    @Test
    fun getUsersTest() {
        // given
        val userList = listOf(
            User("유녕진1", 27),
            User("유녕진2", null)
        )
        userRepository.saveAll(userList)

        // when
        val results = userService.getUsers()

        // then
        assertSoftly(results) {
            it.size shouldBe 2
            it.map { it.name } should containExactlyInAnyOrder("유녕진1", "유녕진2")
            it.map { it.age } should containExactlyInAnyOrder(27, null)
        }
    }

    @Test
    fun updateUserNameTest() {
        // given
        val target = "update 유녕진"
        val savedUser = userRepository.save(
            User("유녕진", 27)
        )
        val request = UserUpdateRequest(savedUser.id!!, target)

        // when
        userService.updateUserName(request)

        // then
        val result = userRepository.findAll()[0]
        assertSoftly(result) {
            it.name shouldBe target
        }
    }

    @Test
    @DisplayName("유저 삭제가 정상적으로 동작한다.")
    fun deleteUserTest() {
        // given
        val target = "유녕진"
        userRepository.save(User(target, 27))

        // when
        userService.deleteUser(target)

        // then
        val result = userRepository.findByName(target)
        assertSoftly(result) {
            it shouldBe null
        }
    }

    @Test
    @DisplayName("대출 기록이 없는 유저도 응답에 포함된다.")
    fun getUserLoanHistoriesTest1() {
        // given
        userRepository.save(User("A", null))

        // when
        val results = userService.getUserLoanHistories()

        // then
        assertSoftly(results) {
            results.size shouldBe 1
            results[0].name shouldBe "A"
            results[0].books shouldBe emptyList()
        }
    }

    @Test
    @DisplayName("대출 기록이 많은 유저의 응답이 정상 동작한다.")
    fun getUserLoanHistoriesTest2() {
        // given
        val savedUser = userRepository.save(User("A", null))
        userLoanHistoryRepository.saveAll(
            listOf(
                UserLoanHistory.fixture(savedUser, "Book1", UserLoanStatus.LOANED),
                UserLoanHistory.fixture(savedUser, "Book2", UserLoanStatus.LOANED),
                UserLoanHistory.fixture(savedUser, "Book3", UserLoanStatus.RETURNED),
            )
        )

        // when
        val results = userService.getUserLoanHistories()

        // then
        assertSoftly(results) {
            it.size shouldBe 1
            it[0].name shouldBe "A"
            it[0].books.size shouldBe 3
            it[0].books.map { it.name } should containExactlyInAnyOrder("Book1", "Book2", "Book3")
            it[0].books.map { it.isReturn } should containExactlyInAnyOrder(false, false, true)

        }
    }

}