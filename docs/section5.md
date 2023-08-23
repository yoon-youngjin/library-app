# 5. 세 번째 요구사항 추가하기 - 책 통계

**목표**

1. SQL의 다양한 기능들 (sum, avg, count, group by, order by)을 이해한다.
2. 간결한 함수형 프로그래밍 기법을 사용해보고 익숙해진다.
3. 동일한 기능을 애플리케이션과 DB로 구현해보고, 차이점을 이해한다.

## 책 통계 보여주기 - 프로덕션 코드 개발

**책 통계 화면**
- 현재 대여 중인 책이 몇 권인지 보여준다.
- 분야별로 도서관에 등록되어 있는 책이 각각 몇 권인지 보여준다.

<img width="485" alt="image" src="https://github.com/yoon-youngjin/library-app/assets/83503188/396146df-c3f5-4c86-a585-8925effed57f">

<img width="572" alt="image" src="https://github.com/yoon-youngjin/library-app/assets/83503188/94ba3645-6b2c-40e4-baa8-39a2082afa37">

```kotlin
@Service
@Transactional(readOnly = true)
class BookService(
    private val bookRepository: BookRepository,
    private val userRepository: UserRepository,
    private val userLoanHistoryRepository: UserLoanHistoryRepository,
) {

    ...

    fun countLoanedBook(): Int {
        return userLoanHistoryRepository.findAllByStatus(status = UserLoanStatus.LOANED).size
    }

    fun getBookStatistics(): List<BookStatResponse> {
        val results = mutableListOf<BookStatResponse>()
        val books = bookRepository.findAll()
        for (book in books) {
            results.firstOrNull { dto -> book.type == dto.type }?.plusOne()
                ?: results.add(BookStatResponse(book.type, 1))
        }
        return results
    }

}
```

```kotlin
data class BookStatResponse(
    val type: BookType,
    var count: Int,
) {
    fun plusOne() {
        count++
    }
}
```

## 책 통계 보여주기 - 테스트 코드 개발과 리팩토링



```kotlin
@SpringBootTest
class BookServiceTest @Autowired constructor(
    private val bookService: BookService,
    private val bookRepository: BookRepository,
    private val userLoanHistoryRepository: UserLoanHistoryRepository,
    private val userRepository: UserRepository,
) {

    ...

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
        assertThat(result).hasSize(2)
        assertCount(result, BookType.COMPUTER, 2)
        assertCount(result, BookType.SCIENCE, 1)

//        val computerDto = result.first { result -> result.type == BookType.COMPUTER}
//        assertThat(computerDto.count).isEqualTo(2)
//
//        val scienceDto = result.first { result -> result.type == BookType.SCIENCE}
//        assertThat(scienceDto.count).isEqualTo(1)
    }

    private fun assertCount(results: List<BookStatResponse>, type: BookType, count: Int) {
        assertThat(results.first { result -> result.type == type }.count).isEqualTo(count)
    }

}
```


```kotlin
fun getBookStatistics(): List<BookStatResponse> {
        val results = mutableListOf<BookStatResponse>()
        val books = bookRepository.findAll()
        for (book in books) {
            results.firstOrNull { dto -> book.type == dto.type }?.plusOne()
                ?: results.add(BookStatResponse(book.type, 1))
        }
        return results
    }

data class BookStatResponse(
    val type: BookType,
    var count: Int,
) {
    fun plusOne() {
        count++
    }
}
```

**기존 코드의 문제점**
1. `var count`와 같은 가변 필드로 인해서 실수할 여지가 생긴다.
2. `mutableListOf<BookStatResponse>()`와 같은 가변 리스트가 존재한다. 실수 발생 여지
3. `firstOrNull?...map` 처럼 call chain으로 인해 유지보수나 코드 이해가 어려워질 수 있다.

```kotlin
fun getBookStatistics(): List<BookStatResponse> {
    return bookRepository.findAll() // List<Book>
        .groupBy { book -> book.type } // Map<BookType, List<Book>>
        .map { (type, books) -> BookStatResponse(type, books.size) } // List<BookStatResponse>

}
```

## 애플리케이션 대신 DB로 기능 구현하기

### 대출 권수 - 기존과 어떤 차이가 있을까?

```kotlin
interface UserLoanHistoryRepository : JpaRepository<UserLoanHistory, Long> {

    fun findByBookNameAndStatus(bookName: String, status: UserLoanStatus): UserLoanHistory?

    fun findAllByStatus(status: UserLoanStatus): List<UserLoanHistory>

    fun countByStatus(status: UserLoanStatus): Long
    
}
```

```kotlin
    fun countLoanedBook(): Int {
//        return userLoanHistoryRepository.findAllByStatus(status = UserLoanStatus.LOANED).size
        return userLoanHistoryRepository.countByStatus(status = UserLoanStatus.LOANED).toInt()
    }
```

기존 코드와 어떤 차이가 있고, 뭐가 좋을까?

그것을 알기 위해서는 서버 코드를 보고 Query를 생각할 수 있어야 한다
`findAllByStatus`는 `select * from user_loan_history where status = ?;`와 같은 쿼리가 나간다. 

또한, 서버 코드를 보고 서버의 메모리를 생각할 수 있어야 한다.
위와 같은 쿼리가 수행되면 서버에 DB에 있는 모든 UserLoanHistory가 List에 존재한다.

즉, 코드 변경 전 방법은
1. DB에 존재하는 데이터를 모두 메모리로 가져와서
2. 애플리케이션(메모리 상에서)이 그 size를 계산한다.

반면에,

`countByStatus`는 `select count(*) from user_loan_history where status =?`와 같은 쿼리가 수행된다.
DB의 결과로 서버 메모리 상에 상수가 올라온다.

즉, 코드 변경 후 방법은
1. DB로부터 숫자를 가져와서
2. 적절히 타입을 변환해준다.

**어떤 방법이 더 좋을까?**
- 첫 번째 방법은 전체 데이터 쿼리를 메모리에 로딩하여 size를 계산
- 두 번째 방법은 count 쿼리 수행 후 타입 변환

일반적으로는 후자가 좋다. 
그 이유로는 DB 및 Network 부하, 애플리케이션 부하가 덜 든다. 

### 분야별 통계 - 기존과 어떤 차이가 있을까?

```kotlin
interface BookRepository : JpaRepository<Book, Long> {
    fun findByName(bookName: String): Book?

    @Query("SELECT NEW com.group.libraryapp.dto.book.response.BookStatResponse(b.type, COUNT(b.id)) "+ 
        "FROM Book b GROUP BY b.type")
    fun getStats(): List<BookStatResponse>
}

data class BookStatResponse(
    val type: BookType,
    val count: Long,
)
```

```kotlin
 fun getBookStatistics(): List<BookStatResponse> {
        return bookRepository.getStats()
//        return bookRepository.findAll() // List<Book>
//            .groupBy { book -> book.type } // Map<BookType, List<Book>>
//            .map { (type, books) -> BookStatResponse(type, books.size) } // List<BookStatResponse>
    }
```

**기존과 어떤 차이가 있을까?**
- 첫 번째는 책 데이터를 메모리 상에 모두 가져온 다음에 함수형 프로그래밍을 수행한다.
- 두 번째는 group by 쿼리를 통해 필요한 데이터만 가져온다.

일반적으로 두 번째 방법이 더 좋다.
그 이유는 (상황에 따라 다르지만) Network 부하, 애플리케이션 부하가 덜 든다. 인덱스를 이용해 튜닝할 여지가 있다.

