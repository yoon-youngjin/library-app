package com.group.libraryapp.domain.book

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
// 현재 Book 클래스에는 주생성자 하나만 존재하고, 파라미터가 없는 기본 생성자가 없기 때문에 에러가 발생한다.
// 이 에러를 해결해주기 위해서는 다음과 같은 kotlin-jpa 플러그인이 필요하다.
class Book(
    val name: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null, // 변경 가능성 X -> val
) {
    init {
        if (name.isBlank()) {
            throw IllegalStateException("이름은 비어 있을 수 없습니다")
        }
    }

}