# 2. Kotlin 리팩토링 계획 세우기

## 도메인 계층을 Kotlin으로 변경하기 - Book.java, UserLoanHistory.java, User.java

```kotlin
@Entity

class Book(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null, // 변경 가능성 X -> val
    val name: String,
) {
    init {
        if (name.isBlank()) {
            throw IllegalStateException("이름은 비어 있을 수 없습니다")
        }
    }

}
```

- 현재 Book 클래스에는 주생성자 하나만 존재하고, 파라미터가 없는 기본 생성자가 없기 때문에 에러가 발생한다.
    - 이 에러를 해결해주기 위해서는 다음과 같은 kotlin-jpa 플러그인이 필요하다.

`build.gradle`

```groovy
id 'org.jetbrains.kotlin.plugin.jpa' version '1.6.21'
```

BookServiceTest를 실행하면 모든 테스트까 깨지는 것을 확인할 수 있다.

```text
nested exception is java.lang.NoClassDefFoundError: kotlin/reflect/full/KClasses
```

해당 에러는 코틀린 클래스에 대한 리플렉션을 할 수 없어 발생하는데, 이를 해결하기 위해서는 Kotlin 리플렉션 라이브러리를 넣어줘야 한다.

```groovy
dependencies {
    ...
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.21")
}
```

```kotlin
@Entity
class UserLoadHistory(

    @ManyToOne
    val user: User,

    val bookName: String,

    var isReturn: Boolean,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {

    fun doReturn() {
        this.isReturn = true
    }

}
```

```kotlin
@Entity
class User(

    var name: String,

    val age: Int?,

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val userLoanHistories: MutableList<UserLoadHistory> = mutableListOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    init {
        if (this.name.isBlank()) {
            throw IllegalStateException("이름은 비어 있을 수 없습니다")
        }
    }

    fun updateName(name: String) {
        this.name = name
    }

    fun loadBook(book: Book) {
        this.userLoanHistories.add(UserLoadHistory(this, book.name, false))
    }

    fun returnBook(bookName: String) {
        this.userLoanHistories.first {
            it.bookName == bookName
        }.doReturn()
    }
}
```

## Kotlin과 JPA를 함께 사용할 때 이야기거리 3가지

### setter에 관한 이야기

```kotlin
@Entity
clase User (
var name: String,
...
) {
    ...
    fun updateName(name: String) {
        this.name = name
    }
}
```

- 생성자 안의 var 프로퍼티
- setter 대신 추가적인 함수

var 프로퍼티가 public으로 열려있기 때문에 setter를 쓸 수 있지만, setter 대신 좋은 이름의 함수를 사용하는 것이 훨씬 clean하다.
하지만 name에 대한 setter는 public이기 때문에 유저 이름 업데이트 기능에서 setter를 사용할 수도 있다.

이렇게 코드 상 setter를 사용할 수도 있다는 것이 불편하다. public getter는 필요하기 때문에 setter만 private하게 만드는 것이 최선이다.

**첫번째 방법 - backing property 사용하기**

```kotlin
class User(
    private var _name: String,
) {
    val name: String
        get() = this._name
}
```

`private var _name`으로 두고, 외부에는 마치 커스텀 getter를 사용한 name을 열어주는 방법이다. 내부에서는 `_name`에 접근해서 값을 변경하고, 외부에서는 불변인 `name`에 접근한다.

**두번째 방법 - custom setter 이용하기**

```kotlin
clas User (
    name: String,
) {
    var name = name
    private set
}
```

생성자에서는 `name`이라는 파라미터만 받게 되고, 해당 파라미터를 클래스 바디에 존재하는 name property에 연결시켜준 뒤 이때 setter를 private으로 설정

하지만 두 방법 모두 프로퍼티가 많아지면 번거롭다는 단점이 존재한다. 때문에 개인적으로 setter를 열어는 두지만 사용하지 않는 방법을 선호한다.

### 생성자 안의 프로퍼티 or 클래스 body 안의 프로퍼티

```kotlin
@Entity
class User(

    var name: String,

    val age: Int?,

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val userLoanHistories: MutableList<UserLoadHistory> = mutableListOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
    ...
}
```

위와 같이 꼭 primary constructor 안에 모든 프로퍼티를 넣어야 할까?

```kotlin
@Entity
class User(

    var name: String,

    val age: Int?,
) {
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val userLoanHistories: MutableList<UserLoadHistory> = mutableListOf()

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    ...
}
```

위와 같은 코드도 정상 동작한다.
모든 프로퍼티를 생성자에 넣거나 프로퍼티를 생성자 혹은 클래스 body 안에 구분해서 넣을 때 **명확한 기준**이 있으면 된다.

### JPA와 data class

결론적으로 Entity는 data class를 피하는 것이 좋다.
`equals`, `hashCode`, `toString` 모두 JPA entity와는 100% 어울리지 않는 메소드이기 때문이다.

예를 들어, User의 `equals`를 호출한다면 User entity 입장에서는 프로퍼티로 UserLoanHistory가 존재하기 때문에 UserEqualsHistory의 equals를 부른다.
또한, UserLoadHistory도 프로퍼티로 User가 존재하기 때문에 User의 equals를 부른다.
이와 같은 상황이 발생할 수 있기 때문에 Entity는 data class를 피하는 것이 좋다.

## 리포지토리를 Kotlin으로 변경하기

```kotlin
interface UserRepository : JpaRepository<User, Long> {

    fun findByName(name: String): Optional<User>
}
```

```kotlin
interface BookRepository : JpaRepository<Book, Long> {

    fun findByName(bookName: String): Optional<Book>
}
```

```kotlin
interface UserLoanHistoryRepository : JpaRepository<UserLoadHistory, Long> {

    fun findByBookNameAndIsReturn(bookName: String, isReturn: Boolean): UserLoadHistory?
}
```

Kotilin에서는 Optional을 사용하지 않아도 null 가능성을 표시할 수 있지만, 우선은 Service 계층의 구현 변경을 최소화하기 위해 Optional을 남겨둔다.

## 서비스 계층을 Kotlin으로 변경하기 - UserService.java

```kotlin
@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
) {


    @Transactional
    fun saveUser(request: UserCreateRequest) {
        val newUser = User(request.name, request.age)
        userRepository.save(newUser)
    }

    fun getUsers(): List<UserResponse> {
        return userRepository.findAll()
//            .map(::UserResponse) // map으로 들어온 user라는 인자를 바로 UserResponse에 생성자로 넣어줄 수도 있다.
            .map { user -> UserResponse(user) }
    }

    @Transactional
    fun updateUserName(request: UserUpdateRequest) {
        val user = userRepository.findById(request.id)
            .orElseThrow(::IllegalArgumentException)
        user.updateName(request.name)
    }

    @Transactional
    fun deleteUser(name: String) {
        val user = userRepository.findByName(name)
            .orElseThrow(::IllegalArgumentException)
        userRepository.delete(user)
    }

}
```

- `@Transactional`에 의해서 UserService, saveUser에 빨간줄(컴파일 에러)가 발생한다.
    - 이유는 트랜잭션 기능을 정상적으로 사용하기 위해서는 saveUser 함수가 오버라이드 될 수 있어야 한다.
    - 하지만 코틀린에서는 기본적으로 클래스가 상속이 막혀있다. 상속하기 위해선 open을 붙여야 한다.
    - 함수도 기본적으로 오버라이드가 막혀 있으므로 오버라이드를 위해서 open을 붙여야 한다.
    - 하지만 모든 클래스나 함수를 open을 붙이는 것은 번거롭기 때문에 플러그인을 통해 처리한다.

```groovy
id 'org.jetbrains.kotlin.plugin.spring' version '1.6.21'
```

- 해당 플러그인을 추가하면 스프링 빈 클래스를 자동으로 열어주고, 그 안의 메서드들도 자동으로 열어주므로 일일이 open을 붙이지 않아도 된다.

## BookService.java를 Kotlin으로 변경하고 Optional 제거하기

```kotlin
interface BookRepository : JpaRepository<Book, Long> {

    fun findByName(bookName: String): Book?
}
```

```kotlin
@Service
@Transactional(readOnly = true)
class BookService(
    private val bookRepository: BookRepository,
    private val userRepository: UserRepository,
    private val userLoanHistoryRepository: UserLoanHistoryRepository,
) {

    @Transactional
    fun saveBook(request: BookRequest) {
        val book = Book(request.name)
        bookRepository.save(book)
    }

    @Transactional
    fun loanBook(request: BookLoanRequest) {
        val book = bookRepository.findByName(request.bookName) ?: throw IllegalArgumentException()
        if (userLoanHistoryRepository.findByBookNameAndIsReturn(request.bookName, false) != null) {
            throw IllegalArgumentException("진작 대출되어 있는 책입니다")
        }

        val user = userRepository.findByName(request.userName) ?: throw IllegalArgumentException()
        user.loanBook(book)
    }

    @Transactional
    fun returnBook(request: BookReturnRequest) {
        val user = userRepository.findByName(request.userName) ?: throw IllegalArgumentException()
        user.returnBook(request.bookName)
    }

}
```

위 코드에서 `?: throw IllegalArgumentException()`가 반복되고 있다. 이러한 코드는 다음과 같이 리팩토링 할 수 있다.

`ExceptionUtils.kt`

```kotlin
fun fail(): Nothing {
    throw IllegalArgumentException()
}
```

```kotlin
@Service
@Transactional(readOnly = true)
class BookService(
    private val bookRepository: BookRepository,
    private val userRepository: UserRepository,
    private val userLoanHistoryRepository: UserLoanHistoryRepository,
) {

    @Transactional
    fun saveBook(request: BookRequest) {
        val book = Book(request.name)
        bookRepository.save(book)
    }

    @Transactional
    fun loanBook(request: BookLoanRequest) {
        val book = bookRepository.findByName(request.bookName) ?: fail()
        if (userLoanHistoryRepository.findByBookNameAndIsReturn(request.bookName, false) != null) {
            throw IllegalArgumentException("진작 대출되어 있는 책입니다")
        }

        val user = userRepository.findByName(request.userName) ?: fail()
        user.loanBook(book)
    }
    ...
}
```

```kotlin
@Transactional
fun updateUserName(request: UserUpdateRequest) {
    val user = userRepository.findById(request.id)
        .orElseThrow(::IllegalArgumentException)
    user.updateName(request.name)
}
```

현재 위와 같이 CrudRepository의 `findById()`와 같이 직접 제어할 수 없는 메서드가 남아있다.
이러한 경우 코틀린의 확장함수를 통해 처리할 수 있다. 스프링 프레임워크는 코틀린과 CrudRepository를 함께 사용할 때를 대비해서 CrudRepositoryExtensions.kt을 미리 만들어두었다.

<img width="794" alt="image" src="https://github.com/yoon-youngjin/library-app/assets/83503188/d4d4beb7-0788-4d3b-8de2-cb58fc187a58">

```kotlin
    @Transactional
fun updateUserName(request: UserUpdateRequest) {
    val user = userRepository.findByIdOrNull(request.id) ?: fail()
    user.updateName(request.name)
}
```

여기서 한 단계 더 나아가서 `.findByIdOrNull(request.id) ?: fail()` 까지를 확장함수를 통해 자체적으로 만들어줄 수 있다.

`ExceptionUtils.kt`

```kotlin
fun <T, ID> CrudRepository<T, ID>.findByIdOrThrow(id: ID): T {
    return this.findByIdOrNull(id) ?: fail()
}
```

- this : CrudRepository

CrudRepository라는 기존에 존재하던 자바 인터페이스를 원본으로 하여 마치 CrudRepository안의 메서드처럼 사용할 수 있게 되었다.

```kotlin
    @Transactional
fun updateUserName(request: UserUpdateRequest) {
    val user = userRepository.findByIdOrThrow(request.id)
    user.updateName(request.name)
}
```

## DTO를 Kotlin으로 변경하기

```kotlin
data class UserResponse(
    val id: Long,
    val name: String,
    val age: Int?,
) {
    companion object {
        fun of(user: User): UserResponse {
            return UserResponse(
                id = user.id!!,
                name = user.name,
                age = user.age
            )
        }
    }
}
```

```kotlin
    fun getUsers(): List<UserResponse> {
    return userRepository.findAll()
        .map { user -> UserResponse.of(user) }
}
```

DTO의 경우는 class 대신 data class가 적합하다. 현재는 equals, hashCode, toString을 사용하고 있지 않지만 DTO의 의미를 생각해 보았을 때 언제라도 사용될 수 있기 때문이다.

## Controller 계층을 Kotlin으로 변경하기

```kotlin
@RestController
class UserController(
    private val userService: UserService,
) {

    @PostMapping("/user")
    fun saveUser(@RequestBody request: UserCreateRequest) {
        userService.saveUser(request)
    }

    @GetMapping("/user")
    fun getUsers(): List<UserResponse> {
        return userService.getUsers()
    }

    @PutMapping("/user")
    fun updateUserName(@RequestBody request: UserUpdateRequest) {
        return userService.updateUserName(request)
    }

    @DeleteMapping("/user")
    fun deleteUser(@RequestParam name: String) {
        return userService.deleteUser(name)
    }
}
```

```kotlin
@RestController
class BookController(
    private val bookService: BookService,
) {

    @PostMapping("/book")
    fun saveBook(@RequestBody request: BookRequest) {
        bookService.saveBook(request)
    }

    @PostMapping("/book/loan")
    fun loanBook(@RequestBody request: BookLoanRequest) {
        bookService.loanBook(request)
    }

    @PutMapping("/book/return")
    fun returnBook(@RequestBody request: BookReturnRequest) {
        bookService.returnBook(request)
    }
}
```

아래와 같은 코드도 가능하다.

```kotlin
@DeleteMapping("/user")
fun deleteUser(@RequestParam name: String) = userService.deleteUser(name)
```

추가적으로 `@RequestParam`은 required가 기본적으로 true이므로 반드시 값이 필요하다.
이때 `@RequestParam name: String?`이라고 적어주면, 스프링이 자체적으로 null이 가능함을 파악하고, required값을 false로 변경한다.

```kotlin
@SpringBootApplication
class LibraryAppApplication

fun main(args: Array<String>) {
    runApplication<LibraryAppApplication>(*args)
}
```
- 코틀린에서는 탑 라인에 여러 클래스와 여러 함수를 만들 수 있고, 함수를 만들 경우 static으로 간주된다.

UI 기능을 확인하는 과정에 아래와 같은 오류를 마주할 수 있다.

```text
Resolved [org.springframework.http.converter.HttpMessageNotReadableException: JSON par
se error: Cannot construct instance of `com.group.libraryapp.dto.book.request.BookRequ
est` (although at least one Creator exists): cannot deserialize from Object value (no
delegate- or property-based Creator); nested exception is com.fasterxml.jackson.datab
ind.exc.MismatchedInputException: Cannot construct instance of `com.group.libraryapp.d
to.book.request.BookRequest` (although at least one Creator exists): cannot deserializ
e from Object value (no delegate- or property-based Creator)<EOL> at [Source: (org.spr
ingframework.util.StreamUtils$NonClosingInputStream); line: 1, column: 2]]
```

Kotlin Jackson Module을 의존성에 추가해 해결할수 있다! 이 에러는 Jackson이 Kotlin Class를 생성하지 못해 발생하는 에러이다.

```groovy
implementation 'com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3'
```
