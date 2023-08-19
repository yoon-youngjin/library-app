package com.group.libraryapp.calculator

import java.lang.Exception
import java.lang.IllegalArgumentException

fun main() {
    val calculatorTest = CalculatorTest()
    calculatorTest.addTest()
    calculatorTest.divideTest()
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