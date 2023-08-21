# 4. 두 번째 요구사항 추가하기 - 도서 대출 현황

**목표**

1. join 쿼리의 종류와 차이점을 이해한다.
2. JPA N + 1 문제가 무엇이고 발생하는 원인을 이해한다.
3. N + 1 문제를 해결하기 위한 방법을 이해하고 활용할 수 있다.
4. 새로운 API를 만들 때 생길 수 있는 고민 포인트를 이해하고 적절한 감을 잡을 수 있다.

## 유저 대출 현황 보여주기 - 프로덕션 코드 개발

**유저 대출 현황 화면**
- 유저 대출 현황을 보여준다.
- 과거에 대출했던 기록과 현재 대출 중인 기록을 보여준다.
- 아무런 기록이 없는 유저도 화면에 보여져야 한다.

**API 스펙**

<img width="329" alt="image" src="https://github.com/yoon-youngjin/library-app/assets/83503188/6b43b600-374a-49fe-b83f-aca2a31740ec">

### 새로운 API를 만들 때 코드의 위치를 어떻게 해야 할까?

1. 새로운 Controller를 만들어야 할까?
2. 기존의 Controller에 추가해야 할까?
3. 기존의 Controller에 추가한다면, 어디에 추가해야할끼?

**Controller를 구분하는 기준**
- 화면에서 사용되는 API끼리 모아 둔다
  - 장점 : 화면에서 어떤 API가 사용되는지 한 눈에 알기 용이하다
  - 단점 : 한 API가 여러 화면에서 사용되면 위치가 애매하다 / 서버 코드가 화면에 종속적이다.
- 동일한 도메인끼리 API를 모아 둔다
  - 장점 : 화면 위치와 무관하게 서버 코드는 변경되지 않아도 된다 / 비슷한 API끼리 모이게 되며 코드의 위치를 예측할 수 있다.
  - 단점 : API가 어디서 사용되는지 서버 코드만 보고 알기는 어렵다.

프로젝트가 낯선 사람 입장에서 어떤 기능에 대한 코드가 어디 있는지 찾을 수 있는 것이 목표이다. 
Controller를 찾으면 사용되는 Service, Repository, Domain을 확인할 수 있다. 
즉, 프로젝트가 낯선 사람 입장에서 Controller가 어디 있는지 찾을 수 있어야 한다. 

```kotlin
data class UserLoanHistoryResponse(
    val name: String,
    val books: List<BookHistoryResponse>,
)

data class BookHistoryResponse(
    val name: String,
    val isReturn: Boolean,
)
```

```kotlin
class UserController(
    private val userService: UserService,
) {

    ...

    @GetMapping("/user/loan")
    fun getUserHistories(): List<UserLoanHistoryResponse> {
        return userService.getUserLoanHistories()
    }
}

class UserService(
  private val userRepository: UserRepository,
) {

...

  fun getUserLoanHistories(): List<UserLoanHistoryResponse> {
    val users = userRepository.findAll()
    return users.map { user ->
      UserLoanHistoryResponse(
        name = user.name,
        books = user.userLoanHistories.map { history ->
          BookHistoryResponse(
            name = history.bookName,
            isReturn = history.status == UserLoanStatus.RETURNED
          )
        }
      )
    }
  }

}
```

## 유저 대출 현황 보여주기 - 테스트 코드 개발

**무엇을 검증해야 할까?**
1. 사용자가 지금까지 한 번도 책을 빌리지 않은 경우 API 응답에 잘 포함되어 있어야 한다
2. 사용자가 책을 빌리고 아직 반납하지 않은 경우 isReturn이 false
3. 사용자가 책을 빌리고 반납한 경우 isReturn 값이 true
4. 사용자가 책을 여러권 빌렸는데, 반납을 한 책도 있고 하지 않은 책도 있는 경우 중첩된 리스트에 여러 권이 정상적으로 들어가 있어야 한다

```kotlin
@SpringBootTest
class UserServiceTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val userLoanHistoryRepository: UserLoanHistoryRepository,
) {
    ...

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
            UserLoadHistory.fixture(savedUser, "Book1", UserLoanStatus.LOANED),
            UserLoadHistory.fixture(savedUser, "Book2", UserLoanStatus.LOANED),
            UserLoadHistory.fixture(savedUser, "Book3", UserLoanStatus.RETURNED),
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
```

## N + 1 문제와 N + 1 문제가 발생하는 이유

```kotlin
  fun getUserLoanHistories(): List<UserLoanHistoryResponse> {
  val users = userRepository.findAll()
  return users.map { user ->
    UserLoanHistoryResponse(
      name = user.name,
      books = user.userLoanHistories.map { history ->
        BookHistoryResponse(
          name = history.bookName,
          isReturn = history.status == UserLoanStatus.RETURNED
        )
      }
    )
  }
}
```
- `userRepository.findAll()` : user 테이블의 모든 정보를 가져온다. -> select * from user;
- `user.userLoanHistories.map` : userLoanHistory 테이블에 접근해서 정보를 가져온다. -> select * from user_loan_history where user_id = 1
- 즉, 최초에 모든 유저를 가져오고 (쿼리 1회) Loop를 통해 유저별로 히스토리를 가져온다. (쿼리 N회)
- 유저가 많아지면 문제가 발생할 수 있다. 

최초에 데이터를 1번 가져온 이후에 해당 데이터를 토대로 다시 N번 쿼리가 추가로 발생하는 것은 **N + 1 문제**라고 한다.

### JPA 1 : N 연관관계의 동작 원리

최초 유저 로딩시 가짜 List<UserLoanHistory>가 들어간다.
그 이유는 시작부터 모든 진자 히스토리를 들고오는것은 비효율적일 수 있기 때문이다.
실제 히스토리에 접근할때에 진짜 UserLoanHistory를 쿼리를 날려 가져온다. 이러한 전략을 Lazy Fetching이라고 한다.

## N + 1 문제를 해결하는 방법! fetch join

```kotlin
interface UserRepository : JpaRepository<User, Long> {

    fun findByName(name: String): User? // 쿼리 실행 결과가 존재하지 않으면 자동으로 null을 넣어준다.
    
    @Query("select distinct u from User u " +
        "left join fetch u.userLoanHistories")
    fun findAllWithHistories(): List<User>
}
```
- `distinct` : join 특성상 조건에 맞는 경우 모두 행으로 만들기 때문에 같은 user에 대해서 한 건의 데이터만 만들기 위해서 distinct를 사용해야 한다.
- `fetch` : fetch를 명시하면 user를 가져올 때 userLoanHistory에 대한 실제 데이터도 함께 가져올 수 있다.

## 조금 더 깔끔한 코드로 변경하기

```kotlin
class UserService(
    private val userRepository: UserRepository,
) {

    ...

    fun getUserLoanHistories(): List<UserLoanHistoryResponse> {
        return userRepository.findAllWithHistories()
            .map(UserLoanHistoryResponse::of)
    }

}
```

```kotlin
data class UserLoanHistoryResponse(
    val name: String,
    val books: List<BookHistoryResponse>,
) {
    companion object {
        fun of(user: User): UserLoanHistoryResponse {
            return UserLoanHistoryResponse(
                name = user.name,
                books = user.userLoanHistories.map(BookHistoryResponse::of)
            )
        }
    }

}

data class BookHistoryResponse(
    val name: String,
    val isReturn: Boolean,
) {
    companion object {
        fun of(history: UserLoadHistory): BookHistoryResponse {
            return BookHistoryResponse(
                name = history.bookName,
                isReturn = history.isReturn
            )

        }
    }
}
```

```kotlin
@Entity
class UserLoadHistory(
    ...
) {

    val isReturn: Boolean
        get() = this.status == UserLoanStatus.RETURNED

    ...
}
```
