# 3. 첫 번째 요구사항 추가하기 - 책의 분야

**목표**

1. Type, Status 등을 서버에서 관리하는 방법들을 살펴보고 장단점을 이해한다.
2. Test Fixture의 필요성을 느끼고 구성하는 방법을 알아본다.
3. Kotlin에서 Enum + JPA + Spring Boot를 활용하는 방법을 알아본다.

## 책의 분야 추가하기

### 요구사항1 확인

**책 등록 요구사항 추가** 
- 책을 등록할 때에 '분야'를 선택해야 한다.
  - 분야에는 5가지 분야가 있다. - 컴퓨터/경제/사회/언어/과학

```kotlin
@Entity
class Book(
  val name: String,

  val type: String,

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null, // 변경 가능성 X -> val
) {
  init {
    if (name.isBlank()) {
      throw IllegalStateException("이름은 비어 있을 수 없습니다")
    }
  }


  companion object {
    fun fixture(
      name: String = "책 이름",
      type: String = "COMPUTER",
      id: Long? = null,
    ): Book {
      return Book(
        name = name,
        type = type,
        id = id,
      )
    }

  }

}
```
- 엔티티의 프로퍼티가 추가 되었을 경우에 테스트 코드에 영향을 끼치지 않기 위한 방법
  - 생성자를 통해 엔티티를 생성하는 것이 아닌 정적 팩토리 메서드를 사용
  - companion object가 아래에 들어가는 것이 코틀린 컨벤션

```kotlin
    @Test
    @DisplayName("책 반납이 정상 동작한다")
    fun returnBookTest() {
        bookRepository.save(Book.fixture("test"))
          ...
    }
```

위와 같이 변경하면 엔티티의 추가적으로 프로퍼티가 생기더라도 테스트 코드에는 오류가 발생하지 않고, Entity 내부에 fixture만 수정하면 된다.

위와 같은 패턴을 **Object Mother 패턴**이라고 하며, 이렇게 생겨난 테스트용 객체를 **Test Fixture**라고 부른다.

> DTO의 경우에는 해당 API에만 사용되기 때문에 test fixture를 만들기도 하고, 만들지 않기도 한다.

## Enum Class를 활용해 책의 분야 리팩토링 하기

**기존 구조의 문제점 1**

현재 로직에서는 올바른 요청이 오는지 검증하지 않고 있다.
만약에 Book의 타입에 오타가 발생하는 경우 잘못된 값 그대로 저장된다.

**기존 구조의 문제점 2**

코드만 보았을 때, DB 테이블에 어떤 값이 들어가는지 알 수 없다. 

**기존 구조의 문제점 3**

type과 관련된 새로운 로직을 작성할 때 번거롭다.
예를 들어, 책을 대출할 때마다 분야별로 '이벤트 점수'를 준다면?

<img width="546" alt="image" src="https://github.com/yoon-youngjin/library-app/assets/83503188/6b03798f-9e2f-4f9c-be9e-fc3ec0706445">

위와 같은 코드는 코드에 분기가 들어가고, 실행되지 않은 else문이 존재하며, 문자열 타이핑은 실수할 여지가 많고, 새로운 type이 생기는 경우 로직 추가를 놓칠 수 있다.
이러한 단점을 해결하기 위해 enum class를 활용하자.

enum class는 3가지 위치에 둘 수 있는데,
1. Book 도메인이 존재하는 패키지
2. Book 도메인 내부
3. Type 패키지를 별도로 분리해서 그 안에 Book 패키지를 새로 생성하는 방 

```kotlin
enum class BookType {
    COMPUTER,
    ECONOMY,
    SOCIETY,
    LANGUAGE,
    SCIENCE,
}
```

```kotlin
@Entity
class Book(
    val name: String,

    val type: BookType,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null, // 변경 가능성 X -> val
) {
    init {
        if (name.isBlank()) {
            throw IllegalStateException("이름은 비어 있을 수 없습니다")
        }
    }

    companion object {
        fun fixture(
            name: String = "책 이름",
            type: BookType = BookType.COMPUTER,
            id: Long? = null,
        ): Book {
            return Book(
                name = name,
                type = type,
                id = id,
            )
        }

    }

}
```

```kotlin
data class BookRequest(
    val name: String,
    val type: BookType,
)
```
- 위와 같이 enum class를 프로퍼티로 두면, 문자열로 COMPUTER, ECONOMY, ... 가 들어오는지 자동으로 검증해준다.

책을 대출할 때마다 분야별로 '이벤트 점수'를 주는 처리는 아래와 같이 바뀔 수 있다.

<img width="395" alt="image" src="https://github.com/yoon-youngjin/library-app/assets/83503188/e9aeade8-affc-4d37-bbbb-c057a4c2c6c6">

<img width="380" alt="image" src="https://github.com/yoon-youngjin/library-app/assets/83503188/045d9b4a-fd08-427a-afa9-b7e27781fba4">

1. 다형성을 활용해 코드에 분기가 없다.
2. 실행되지 않을 else 문도 제거하되 함수가 깔끔해졌다.
3. BookType 클래스에 score를 위임해 문자열 타이핑이 사라졌다.
4. 새로운 Type이 추가될 때 score를 빠뜨릴 수 없기 때문에 유지보수가 수월하다.

**Enum을 사용하면 DB에 어떤 값이 들어갈까?**

현재는 순서가 들어간다. (0, 1, 2, 3, ...)

```kotlin
class Book(
    val name: String,

    @Enumerated(EnumType.STRING)
    val type: BookType,
...
```


## Boolean에도 Enum활용하기 - 책 반납 로직 수정

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

위의 Entity에서 `isReturn` 변수의 타입인 Boolean도 Enum을 활용해볼 수 있다.

예를 들어, User 테이블이 있다고 생각해보자. 

```kotlin
class User(
    val name: String,
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
```

만약에 새로운 요구사항으로 휴면 여부를 관리해달라는 요구사항이 추가된 경우에

```kotlin
class User(
    val name: String,
    
    var isActive: Boolean,
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
```

위와 같이 Flag 변수를 통해 true이면 휴면 유저가 아니고, false인 경우 휴먼 유저로 구분해볼 수 있다.

한달 후에, 새로운 요구사항으로 유저의 탈퇴 여부를 soft하게 관리해달라는 요구사항과 탈퇴는 휴먼을 해제하여 로그인 한 후에 이루어진다고 하자.

```kotlin
class User(
    val name: String,
    
    var isActive: Boolean,
    
    var isDeleted: Boolean,
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
```

추가적으로 Flag 변수를 통해 true이면 털퇴 유저, false인 경우 탈퇴 유저가 아님으로 구분해볼 수 있다.
이렇게 Boolean이 2개가 되면 문제가 생긴다.
1. Boolean이 2개 있기 때문에 코드가 이해하기 어려워진다.
   - 한 객체가 여러 상태를 표현할 수록 이해하기 어렵다.
   - 경우의 수가 너무 많이 생긴다.
2. Boolean 2개로 표현되는 4가지 상태가 모두 유의미하지 않다.
   - (isActive, isDeleted)는 총 4가지 경우가 있다.
     - (false, false) - 휴먼 상태인 유저
     - (false, true) - 휴먼 상태이면서 탈퇴한 유저일 수는 없다.
     - (true, false) - 활성화된 유저
     - (true, true) - 탈퇴한 유저
   - 2번째 경우는 DB에 존재할 수 없는 조합이고, 이런 경우가 코드에서 가능한 것은 유지보수를 어렵게 만든다.

이러한 경우 Enum Class를 도입하여 해결할 수 있다.

```kotlin
class User(
    val name: String,

    @Enumerated(EnumType.STRING)
    var state: UserState,
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)

enum class UserState {
    ACTIVE,
    IN_ACTIVE,
}
```

추가로 탈퇴 유저에 대한 요구사항이 생긴다면

```kotlin
enum class UserState {
    ACTIVE,
    IN_ACTIVE,
    DELETED,
}
```

이렇게 Enum을 활용하게 되면
1. 필드 1개로 여러 상태를 표현할 수 있기 때문에 코드의 이해가 쉬워진다.
2. 정확하게 유의미한 상태만 나타낼 수 있기 때문에 코드의 유지보수가 용이해진다.

```kotlin
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

enum class UserLoanStatus {
    RETURNED, // 반납 되어 있는 상태
    LOANED, // 대출 중인 상태
}
```

위와 같이 구성하면 현재는 반납, 대출 총 2가지 상태만 존재하지만 다양한 상태가 추가되면 대응하기 쉬워진다.

