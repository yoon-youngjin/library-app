package com.group.libraryapp.config

import com.querydsl.jpa.impl.JPAQueryFactory
import javax.persistence.EntityManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class QuerydslConfig(
    private val em: EntityManager,
) {

    @Bean
    fun querydsl(): JPAQueryFactory {
        return JPAQueryFactory(em)
    }
}