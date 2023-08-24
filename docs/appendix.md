# 부록

## 스프링 컴포넌트와 spring 플러그인

```groovy
id 'org.jetbrains.kotlin.plugin.spring' version '1.6.21'
```

@Component, @Async, @Transactional, @Cacheable, @SpringBootTest 어노테이션이 붙어있는 클래스나 함수를 자동으로 open 해준다.
open 함으로써 함수를 오버라이딩, 클래스를 상속 가능하게 해준다.

따라서, 코틀린에서 프록시 개념을 사용할 때 기존의 원본 클래스를 상속하여 프록시 객체를 만드는데, 이를 위해서 open이 되어야 한다.

## JPA 객체와 기본 생성자

```groovy
id 'org.jetbrains.kotlin.plugin.jpa' version '1.6.21'
```

JPA 플러그인의 역할은 Entity 객체, mapped super 클래스 객체, Embeddable 객체에 기본 생성자를 만들어준다.
JPA는 동적으로 Entity 인스턴스를 만들 때 리플렉션 기능을 사용하여 인스턴스화 할 때 기본 생성자가 필요하다.

## JPA 객체와 open

```groovy
id 'org.jetbrains.kotlin.plugin.allopen' version '1.6.21'

allopen {
  annotations("javax.persistence.Entity")
  annotations("javax.persistence.MappedSuperclass")
  annotations("javax.persistence.Embeddable")
}
```

JPA 객체에 대해서 open이 가능하도록 해주는 allopen 플러그인
JPA 객체에서는 지연로딩 기능을 사용할 때 프록시를 사용하기 때문에 동일한 이유로 open되어야 한다.
