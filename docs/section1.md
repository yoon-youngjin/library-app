# 1. 도서관리 애플리케이션 이해하기

## 테스트 코드란 무엇인가, 그리고 왜 필요한가?

> 테스트 코드 : 프로그래밍 코드를 사용해 무엇인가를 검사한다. 즉, 자동으로(사람의 손을 거치지 않고) 테스트 할 수 있다.

### 테스트 코드는 왜 필요한가?

1. 개발 과정에서 문제를 미리 발견할 수 있다.
2. 기능 추가와 리팩토링을 안심하고 할 수 있다.
3. 빠른 시간 내 코드의 동작 방식과 결과를 확인할 수 있다.
4. 좋은 테스트 코드를 작성하려 하다보면, 자연스럽게 좋은 코드가 만들어 진다.
5. 잘 작성한 테스트는 문서 역할을 한다. (코드리뷰를 돕는다.)

### 사칙연산 계산기에 대해 테스트 코드 작성하기

**계산기 요구사항**

1. 계산기는 정수만을 취급한다.
2. 계산기가 생성될 때 숫자를 1개 받는다.
3. 최초 숫자가 기록된 이후에는 연산자 함수를 통해 숫자를 받아 지속적으로 계산한다.

```kotlin
class Calculator(
    var number: Int,
) {

    fun add(operand: Int) {
        this.number += operand
    }

    fun minus(operand: Int) {
        this.number -= operand
    }

    fun multiply(operand: Int) {
        this.number *= operand
    }

    fun divide(operand: Int) {
        if (operand == 0) {
            throw IllegalArgumentException("0으로 나눌 수 없습니다.")
        }
        this.number /= operand
    }
}

class CalculatorTest {

    fun addTest() {
        // given
        val calculator = Calculator(5)

        // when
        calculator.add(3)

        // then
        if (calculator.number != 8) { // data class이므로 equals 자동 구현
            throw IllegalStateException()
        }
    }

    fun minusTest() {
        // given
        val calculator = Calculator(5)

        // when
        calculator.minus(2)

        // then
        if (calculator.number != 3) { // data class이므로 equals 자동 구현
            throw IllegalStateException()
        }
    }

    fun multiplyTest() {
        // given
        val calculator = Calculator(5)

        // when
        calculator.multiply(3)

        // then
        if (calculator.number != 15) { // data class이므로 equals 자동 구현
            throw IllegalStateException()
        }
    }

    fun divideTest() {
        // given
        val calculator = Calculator(5)

        // when
        calculator.divide(2)

        // then
        if (calculator.number != 2) { // data class이므로 equals 자동 구현
            throw IllegalStateException()
        }
    }

    fun divideExceptionTest() {
        // given
        val calculator = Calculator(5)

        // when
        try {
            calculator.divide(0)

        } catch (e: IllegalArgumentException) {
            // 테스트 성공
            if (e.message != "0으로 나눌 수 없습니다.") {
                throw IllegalStateException("메시지가 다릅니다..")
            }
            return
        } catch (_: Exception) {
            throw IllegalStateException()
        }

        throw IllegalStateException("기대하는 예외가 발생하지 않았습니다.")

    }
}
```

테스트 코드는 크게 3부분으로 나누어졌다. (given - when - then 패턴)

1. given : 테스트 대상을 맏르어 준비하는 과정
2. when : 실제 테스트 하고 싶은 기능을 호출하는 과정
3. then : 호출 이후 의도한대로 결과가 나왔는지 확인하는 과정

**수동으로 만든 테스트 코드의 단점**

1. 테스트 클래스와 메소드가 생길 때마다 메인 메소드에 수동으로 코드를 작성해주어야 하고, 메인 메소드가 너무 커진다. 테스트 메소드를 개별적으로 실행하기도 어렵다.
2. 테스트가 실패한 경우 무엇을 기대하였고, 어떤 잘못된 값이 들어와 실패했는지 알려주지 않는다. 예외를 던지거나, try catch를 사용해야 하는 등 직접 구현해야 할 부분이 많아 불편하다.
3. 테스트 메소드별로 공통적으로 처리해야 하는 기능이 있다면, 메소드마다 중복이 생긴다.

위와 같은 단점을 극복하기 위해 **JUnit5**

## Junit5 사용법과 테스트 코드 리팩토링

### JUnit5에서 사용되는 5가지 어노테이션

1. `@Test`: 테스트 메소드를 지정한다. 테스트 메소드를 실행하는 과정에서 오류가 없으면 성공
2. `@BeforeEach`: 각 테스트 메소드가 수행되기 전에 실행되는 메소드를 지정한다.
3. `@AfterEach`: 각 테스트가 수행된 후에 실행되는 메소드를 지정한다.
4. `@BeforeAll`: 모든 테스트를 수행하기 전에 최초 1회 수행되는 메소드를 지정한다.
5. `@AfterAll`: 모든 테스트를 수행한 후 최후 1회 수행되는 메소드를 지정한다.

```kotlin
class JunitTest {

    companion object {

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            println("모든 테스트 시작 전")
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            println("모든 테스트 종료 후")
        }
    }

    @BeforeEach
    fun beforeEach() {
        println("각 테스트 시작 전")
    }

    @AfterEach
    fun afterEach() {
        println("각 테스트 종료 후")
    }

    @Test
    fun test1() {
        println("테스트 1")
    }

    @Test
    fun test2() {
        println("테스트 2")
    }
}
```


```text
모든 테스트 시작 전
각 테스트 시작 전
테스트 1
각 테스트 종료 후
각 테스트 시작 전
테스트 2
각 테스트 종료 후
모든 테스트 종료 후
```

<img width="766" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/b94ee398-4fd1-4a15-9098-92a087c9d8cd">

```kotlin
class JunitCalculatorTest {

    @Test
    fun addTest() {
        // given
        val calculator = Calculator(5)

        // when
        calculator.add(3)

        // then
        assertThat(calculator.number).isEqualTo(8)
    }
}
```
- assertThat(확인하고 싶은 값).isEqualTo(기대값)

**자주 사용되는 단언문 몇 가지**

```kotlin
val isNew = true
assertThat(isNew).isTrue
assertThat(isNew).isFalse
```

주어진 값이 true 인지 / false 인지 검증

```kotlin
val people = listOf(Person("A"), Person("B"))
assertThat(people).hasSize(2)
```

주어진 컬렉션의 size가 원하는 값인지 검증

```kotlin
val people = listOf(Person("A"), Person("B"))
assertThat(people).extracting("name").containsExactlyInAnyOrder("A", "B")
```

주어지 컬렉션 안의 item들에서 name이라는 프로퍼티를 추출한 후, 그 값이 A와 B인지 검증 (이때 순서는 확인하지 않는다)

```kotlin
val people = listOf(Person("A"), Person("B"))
assertThat(people).extracting("name").containsExactly("A", "B")
```

주어지 컬렉션 안의 item들에서 name이라는 프로퍼티를 추출한 후, 그 값이 A와 B인지 검증 (이때 순서도 중요하다)

```kotlin
assertThrows<IllegalArgumentException> {
    function1()
}
```

function1 함수를 실행했을 때 해당 에러가 나오는지 검증

```kotlin
val message = assertThrows<IllegalArgumentException> {
    function1()
}.message
assertThat(message).isEqualTo("잘못된 값이 들어왔습니다.")
```

message를 가져와 예외 메시지 검증

```kotlin
class JunitCalculatorTest {

    @Test
    fun addTest() {
        // given
        val calculator = Calculator(5)

        // when
        calculator.add(3)

        // then
        assertThat(calculator.number).isEqualTo(8)
    }

    @Test
    fun minusTest() {
        // given
        val calculator = Calculator(5)

        // when
        calculator.minus(2)

        // then
        assertThat(calculator.number).isEqualTo(3)
    }

    @Test
    fun multiplyTest() {
        // given
        val calculator = Calculator(5)

        // when
        calculator.multiply(3)

        // then
        assertThat(calculator.number).isEqualTo(15)
    }

    @Test
    fun divideTest() {
        // given
        val calculator = Calculator(5)

        // when
        calculator.divide(2)

        // then
        assertThat(calculator.number).isEqualTo(2)
    }

    @Test
    fun divideExceptionTest() {
        // given
        val calculator = Calculator(5)

        // when, then
        assertThrows<IllegalArgumentException> {
            calculator.divide(0)
        }.apply {
            assertThat(message).isEqualTo("0으로 나눌 수 없습니다.")
        }

    }
}
```

## JUnit5으로 Spring Boot 테스트하기

### 스프링에 존재하는 여러 계층들중에 무엇을 어떻게 테스트 해야 할까?

<img width="573" alt="스크린샷 2023-08-19 오전 12 06 52" src="https://github.com/yoon-youngjin/library-app/assets/83503188/d60bb9d3-cb10-460b-8b67-429d645413da">

- Controller, Service, Repository 계층은 스프링 컨텍스트에 의해 관리되는 Bean
- Domain은 순수한 자바 객체 (POJO)

각 계층은 테스트 방법이 다르다. 
- Domain 계층 : 클래스를 테스트하는 것과 동일
- Service, Repository 계층 : 스프링 빈을 사용하는 테스트 방법 사용 (`@SpringBootTest`)
  - 데이터 위주의 검증
- Controller 계층 : 스프링 빈을 사용하는 테스트 방법 사용 (`@SpringBootTest`)
  - 응답 받은 JSON을 비롯한 HTTP 위주의 검증

### 어떤 계층을 테스트 해야 할까?

당연히 BEST는 모든 계층에 대해 많은 case를 검증하는 것이다. 그럴수록 안정성이 높아지고, 모든 부분에 대해서 자동화된 테스트가 가능하기 때문이다.
하지만 현실적으로 코딩 시간을 고려해 딱 1개 계층만 테스트 한다면 보통 Service 계층이다. 

```kotlin
@SpringBootTest
class UserServiceTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val userService: UserService,
) {
    // saveUser()를 테스트한다는 것은 요청을 보냈을 때 정상적으로 저장되는지를 파악하고 싶은것
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
        assertThat(results[0].age).isNull()
    }
}
```
- 명시적으로 constructor 키워드를 붙이고 그 앞에 @Autowired를 붙이면 각 프로퍼티에 붙이는 @Autowired를 생략할 수 있다.
- `assertThat(results[0].age).isNull()` : 플랫폼 타입에 의해 오류가 발생한다.
  - 자바에서 `getAge()`는 Integer를 반환하는데, 코틀린 입장에서는 Integer가 null이 들어갈 수 있는 정수인지, 없는지 판단이 되지 않기 때문에 오류가 발생한다.
  - 코틀린에서는 우선 null이 아닐 것이라고 판단하고 값을 가져오는데, null이기 때문에 exception이 발생한다.
  - 자바에서 `@Nullable` 을 통해서 해결할 수 있다.

## 유저 관련 기능 테스트 작성하기 

```kotlin
@SpringBootTest
// 명시적으로 constructor 키워드를 붙이고 그 앞에 @Autowired를 붙이면 각 프로퍼티에 붙이는 @Autowired를 생략할 수 있다.
class UserServiceTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val userService: UserService,
) {
    @AfterEach
    fun clean() {
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


    }

    @Test
    fun updateUserNameTest() {
        // given
        val target = "update 유녕진"
        val savedUser = userRepository.save(
            User("유녕진", 27)
        )
        val request = UserUpdateRequest(savedUser.id, target)

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
        assertThat(result).isEmpty()
//        assertThat(userRepository.findAll()).isEmpty()
    }
}
```
- 조회 테스트와 생성 테스트를 한 번에 실행하면 실패한다. 
  - 두 테스트는 spring context를 공유하기 때문이다. -> 하나의 DB만 뜬다.
  - 이런 문제를 해결하기 위해서는 공유 자원인 DB를 정리하면 된다. 
  - `@AfterEach`를 통해 각 테스트가 끝나면 repository를 정리해준다.

## 책 관련 기능 테스트 작성하기

```kotlin
@SpringBootTest
class BookServiceTest @Autowired constructor(
    private val bookService: BookService,
    private val bookRepository: BookRepository,
    private val userLoanHistoryRepository: UserLoanHistoryRepository,
    private val userRepository: UserRepository,
) {

    @AfterEach
    fun clean() {
        bookRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    @DisplayName("책 등록이 정상 동작한다")
    fun saveBookTest() {
        // given
        val request = BookRequest("test")

        // when
        bookService.saveBook(request)

        // then
        val results = bookRepository.findAll()
        assertThat(results).hasSize(1)
        assertThat(results[0].name).isEqualTo("test")
    }

    @Test
    @DisplayName("책 대출이 정상 동작한다")
    fun loanBookTest() {
        // given
        val targetBookName = "target"
        val targetName = "유녕진"
        bookRepository.save(Book(targetBookName))
        val savedUser = userRepository.save(User(targetName, null))
        val request = BookLoanRequest(targetName, targetBookName)

        // when
        bookService.loanBook(request)

        // then
        val results = userLoanHistoryRepository.findAll()
        assertThat(results).hasSize(1)
        assertThat(results[0].bookName).isEqualTo(targetBookName)
        assertThat(results[0].user.id).isEqualTo(savedUser.id)
        assertThat(results[0].user.name).isEqualTo(targetName)
        assertThat(results[0].isReturn).isFalse()
    }

    @Test
    @DisplayName("책이 이미 대출되어 있다면, 신규 대출이 실패한다.")
    fun loanBookFailTest() {
        // given
        bookRepository.save(Book("test"))
        val savedUser = userRepository.save(User("유녕진", null))
        val request = BookLoanRequest("유녕진", "test")
        userLoanHistoryRepository.save(UserLoanHistory(savedUser, "test", false))

        // when then
        assertThrows<IllegalArgumentException> {
            bookService.loanBook(request)
        }.apply {
            assertThat(message).isEqualTo("진작 대출되어 있는 책입니다")
        }
    }

    @Test
    @DisplayName("책 반납이 정상 동작한다")
    fun returnBookTest() {
        bookRepository.save(Book("test"))
        val savedUser = userRepository.save(User("유녕진", null))
        val request = BookReturnRequest(savedUser.name, "test")
        userLoanHistoryRepository.save(UserLoanHistory(savedUser, "test", false))

        // when
        bookService.returnBook(request)

        // then
        val results = userLoanHistoryRepository.findAll()
        assertThat(results[0].isReturn).isTrue()
    }
}

```

> deleteAll(), deleteAllInBatch() 차이 찾아보고, 왜 deleteAllInBatch()는 하위 엔티티 정리안한 경우 오류가 발생하는지 찾아보기
