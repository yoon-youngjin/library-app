# 6. 네 번째 요구사항 추가하기 - Querydsl

**목표**

1. JPQL과 Querydsl의 장단점을 이해할 수 있다.
2. Querydsl을 Kotlin + Spring Boot와 함께 사용할 수 있다.
3. Querydsl을 활용해 기존에 존재하던 Repository를 리팩토링 할 수 있다.

## Querydsl 도입하기

**기술적인 요구사항**
- 현재 사용하는 JPQL은 몇 가지 단점이 있다.
- Querydsl을 적용해서 단점을 극복하자.

### JPQL은 무슨 단점이 있을까?

```kotlin
@Query("select distinct u from User u " +
        "left join fetch u.userLoanHistories")
    fun findAllWithHistories(): List<User>
```
- 문자열이기 때문에 컴파일 시점에 버그를 찾기가 어렵다.
- JPQL 문법이 일반 SQL와 조금 달라 복잡한 쿼리를 작성할 때마다 찾아보아야 한다.

### Spring Data JPA는 무슨 단점이 있을까?

```kotlin
fun findByName(userName: String): User?

fun findByNameAndAge(userName: String, age: Int?): User?

fun findByNameAndAgeAndId(userName: String, age: Int?, id: Long?): User?
```
- 조건이 복잡한 동적쿼리를 작성할 때 함수가 계속해서 늘어난다.
  - 위 예제는 이름은 필수값이고, 선택적인 값(age, id)이 계속 늘어나는 경우
- 프로덕션 코드 변경에 취약하다. (필드명이 변경되면 같이 변경)

이러한 단점을 보완하기 위해 Querydsl이 등장하였다.
Spring Data JPA와 Querydsl을 함께 사용하며 서로를 보완해야 한다.

> Querydsl은 코드로 쿼리를 작성해줄 수 있는 도구

```groovy
plugins {
    ...
    id 'org.jetbrains.kotlin.kapt' version '1.6.21'
}

  ...
dependencies {
    ...
    implementation("com.querydsl:querydsl-jpa:5.0.0")
    kapt("com.querydsl:querydsl-apt:5.0.0:jpa")
    kapt("org.springframework.boot:spring-boot-configuration-processor")


    runtimeOnly 'com.h2database:h2'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
  ...

```


### Querydsl 사용하기 - 첫 번째 방법

<img width="463" alt="image" src="https://github.com/yoon-youngjin/library-app/assets/83503188/5377cbcc-c0a1-4de1-a2a6-c69ccc79ca08">

```kotlin
@Configuration
class QuerydslConfig(
    private val em: EntityManager,
) {

    @Bean
    fun querydsl(): JPAQueryFactory {
        return JPAQueryFactory(em)
    }
}
```

```kotlin
interface UserRepository : JpaRepository<User, Long>, UserRepositoryCustom {

    fun findByName(name: String): User? // 쿼리 실행 결과가 존재하지 않으면 자동으로 null을 넣어준다.

//    @Query("select distinct u from User u " +
//        "left join fetch u.userLoanHistories")
//    fun findAllWithHistories(): List<User>
}
```

```kotlin
interface UserRepositoryCustom {

    fun findAllWithHistories(): List<User>
}
```


```kotlin
class UserRepositoryCustomImpl(
  private val queryFactory: JPAQueryFactory,
) : UserRepositoryCustom {
  override fun findAllWithHistories(): List<User> {
    return queryFactory.select(user).distinct()
      .from(user)
      .leftJoin(userLoanHistory).on(userLoanHistory.user.id.eq(user.id)).fetchJoin()
      .fetch()
  }
}
```
- `select(user)`: select *
- `distinct()`: distinct
- `from(user)`: from user
- `leftJoin(userLoanHistory)`: left join user_loan_history
- `on(userLoanHistory.user.id.eq(user.id))`: on user_loan_history.user_id = user.id
- `fetchJoin()`: 앞의 join을 fetch join으로 간주한다.
- `fetch()`: 쿼리를 수행한다.

- 장점: 하나의 repository만 주입받으면 된다.
- 단점: 인터페이와 클래스를 항상 같이 만들어 주어야 하는 것이 부담이고 여러모로 번거롭다.

## Querydsl 사용하기 - 두 번째 방법

```kotlin
@Component
class BookQuerydslRepository(
    private val queryFactory: JPAQueryFactory,
) {

    fun getStats(): List<BookStatResponse> {
        return queryFactory.select(
            Projections.constructor(
                BookStatResponse::class.java,
                book.type,
                book.id.count()
            )
        )
            .from(book)
            .groupBy(book.type)
            .fetch()
    }

}
```
- `Projections.constructor(...)` : 주어진 DTO의 생성자를 호출한다는 의미이다.
  - Projection은 DB 용어로 *을 통해 모든 컬럼을 가져오는 것이 아닌 특정 필드만을 가져오는것을 의미한다.
  - `select type, count(book.id) from book;`과 동일

```kotlin
class BookService(
    private val bookRepository: BookRepository,
    private val bookQuerydslRepository: BookQuerydslRepository,
    private val userRepository: UserRepository,
    private val userLoanHistoryRepository: UserLoanHistoryRepository,
) {

   ...

    fun getBookStatistics(): List<BookStatResponse> {
        return bookQuerydslRepository.getStats()
    }

}
```

- 장점 : 클래스만 바로 만들면 되어 간결하다.
- 단점 : 필요에 따라 두 Repository를 모두 불러와야한다.

개인적으로는 두 번째 방법을 선호한다.
그 이유는 멀티 모듈을 사용하는 경우, 모듈 별로 각기 다른 Repository를 쓰는 경우가 많아 단점이 상쇄되고 장점이 극대화되기 때문이다.

<img width="543" alt="image" src="https://github.com/yoon-youngjin/library-app/assets/83503188/a918dfdb-612a-41d8-8743-9620dd9dc40d">

- Core 모듈 : 엔티티가 존재하는 모듈
- api 서버나 admin 서버와 같은 모듈

## UserLoanHistoryRepository를 Querydsl으로 리팩토링 하기

```kotlin
interface UserLoanHistoryRepository : JpaRepository<UserLoanHistory, Long> {

    fun findByBookNameAndStatus(bookName: String, status: UserLoanStatus): UserLoanHistory?

    fun findAllByStatus(status: UserLoanStatus): List<UserLoanHistory>

    fun countByStatus(status: UserLoanStatus): Long
}
```

@Query를 사용하지 않은 Repository 기능도 Querydsl로 옮겨야 할까?

개인적으로는 Querydsl로 옮기는 것을 선호한다. 그 이유는 동적 쿼리 간편함 때문이다.

```kotlin
interface UserLoanHistoryRepository : JpaRepository<UserLoanHistory, Long> {

    fun findByBookName(bookName: String): UserLoanHistory?
    
    fun findByBookNameAndStatus(bookName: String, status: UserLoanStatus): UserLoanHistory?

    fun findAllByStatus(status: UserLoanStatus): List<UserLoanHistory>

    fun countByStatus(status: UserLoanStatus): Long
}
```

예를 들어 `findByBookName`이라는 함수가 존재하는 경우 `findByBookNameAndStatus` 함수로 인해서 status는 필수값이 아닌 선택값인 것을 알 수 있다.
이를 통해서 필드가 늘어나고, 조건이 복잡해지면 함수가 많이 눌어날 수 있음을 짐작할 수 있다.

A 필드는 필수적으로 들어오며, B, C, D, E 필드는 선택적으로 들어오는 경우?
- findByA
- findByAAndB
- findByAAndBAndC
- ...

위와 같이 where 조건이 동적으로 바뀌는 쿼리는 Querydsl을 이용하면 쉽게 구현 가능하다.

```kotlin
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
```
- status가 null이 아닌 경우에만 `user_loan_history = ?` 이 실행된다.
  - where 조건에 null이 들어오면 무시하게 된다.
- where에 여러 조건이 들어오면 각 조건은 And로 묶인다.
- `userLoanHistory.count()`는 SQL의 `count(id)`로 변경된다.
- count의 결과는 1개이므로 `fetchOne`



