package com.group.testfixture

import com.group.libraryapp.domain.user.User
import kotlin.random.Random
import kotlin.reflect.KClass
import io.kotest.property.RandomSource

inline fun <reified T : Any> rand(seed: Int? = null): T = random(seed)
inline fun <reified T : Any> random(seed: Int? = null): T = random(T::class, seed)

@Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
fun <T : Any> random(kClass: KClass<T>, seed: Int?): T {
    val random = Random(seed)
    val randomSource = RandomSource(seed)

    return when (kClass) {
        // affiliate common types
//        UserLoanStatus::class -> UserLoanStatus.values().random(random)
//        User::class -> User(
//            name = "유녕진",
//            age = 10,
//        )


        else -> kClass.java.enumConstants.run {
            if (this != null) {
                this.random(random)
            } else {
                throw UnsupportedOperationException("$kClass is not supported")
            }
        }

    }
}

fun Random(seed: Int?) = seed?.let { Random(seed) } ?: Random.Default

fun RandomSource(seed: Int?) = seed?.let { RandomSource.seeded(it.toLong()) } ?: RandomSource.default()

