package com.group.libraryapp.service.user

import com.group.libraryapp.domain.user.User
import com.group.libraryapp.domain.user.UserRepository
import com.group.libraryapp.domain.user.loadhistory.UserLoanHistory
import com.group.libraryapp.domain.user.loadhistory.UserLoanHistoryRepository
import com.group.libraryapp.domain.user.loadhistory.UserLoanStatus
import com.group.libraryapp.dto.user.request.UserCreateRequest
import com.group.libraryapp.dto.user.request.UserUpdateRequest
import org.assertj.core.api.Assertions.assertThat
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
    // saveUser()를 테스트한다는 것은 요청을 보냈을 때 정상적으로 저장되는지를 파악하고 싶은것

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
        assertThat(results).hasSize(1)
        assertThat(results[0].name).isEqualTo("유녕진")
        assertThat(results[0].age).isNull() // 플랫폼 타입에 의해 오류가 발생한다.
        // 자바에서 getAge()는 Integer를 반환하는데, 코틀린 입장에서는 Integer가 null이 들어갈 수 있는 정수인지, 없는지 판단이 되지 않기 때문에 오류가 발생한다.
        // 코틀린에서는 우선 null이 아닐 것이라고 판단하고 값을 가져오는데, null이기 때문에 exception이 발생한다.
        // 자바에서 @Nullable 을 통해서 해결할 수 있다.
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
        assertThat(results).hasSize(2)
        assertThat(results).extracting("name").containsExactlyInAnyOrder("유녕진1", "유녕진2")
        assertThat(results).extracting("age").containsExactlyInAnyOrder(27, null)

        // 조회 테스트와 생성 테스트를 한 번에 실행하면 실패한다.
        // 두 테스트는 spring context를 공유하기 때문이다. -> 하나의 DB만 뜬다.
        // 이런 문제를 해결하기 위해서는 공유 자원인 DB를 정리하면 된다.
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
        assertThat(result.name).isEqualTo(target)
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
        assertThat(result).isNull()
//        assertThat(userRepository.findAll()).isEmpty()
    }

    @Test
    @DisplayName("대출 기록이 없는 유저도 응답에 포함된다.")
    fun getUserLoanHistoriesTest1() {
        // given
        userRepository.save(User("A", null))

        // when
        val results = userService.getUserLoanHistories()

        // then
        assertThat(results).hasSize(1)
        assertThat(results[0].name).isEqualTo("A")
        assertThat(results[0].books).isEmpty()
    }

    @Test
    @DisplayName("대출 기록이 많은 유저의 응답이 정상 동작한다.")
    fun getUserLoanHistoriesTest2() {
        // given
        val savedUser = userRepository.save(User("A", null))
        userLoanHistoryRepository.saveAll(listOf(
            UserLoanHistory.fixture(savedUser, "Book1", UserLoanStatus.LOANED),
            UserLoanHistory.fixture(savedUser, "Book2", UserLoanStatus.LOANED),
            UserLoanHistory.fixture(savedUser, "Book3", UserLoanStatus.RETURNED),
        ))

        // when
        val results = userService.getUserLoanHistories()

        // then
        assertThat(results).hasSize(1)
        assertThat(results[0].name).isEqualTo("A")
        assertThat(results[0].books).hasSize(3)
        assertThat(results[0].books).extracting("name")
            .containsExactlyInAnyOrder("Book1", "Book2", "Book3")
        assertThat(results[0].books).extracting("isReturn")
            .containsExactlyInAnyOrder(false, false, true)
    }

}