package com.group.libraryapp.domain.book

import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
// 현재 Book 클래스에는 주생성자 하나만 존재하고, 파라미터가 없는 기본 생성자가 없기 때문에 에러가 발생한다.
// 이 에러를 해결해주기 위해서는 다음과 같은 kotlin-jpa 플러그인이 필요하다.
class Book(
    val name: String,

    @Enumerated(EnumType.STRING)
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

    // 엔티티의 프로퍼티가 추가 되었을 경우에 테스트 코드에 영향을 끼치지 않기 위한 방법
    // 생성자를 통해 엔티티를 생성하는 것이 아닌 정적 팩토리 메서드를 사용
    // companion object가 아래에 들어가는 것이 코틀린 컨벤션
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

