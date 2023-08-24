package com.group.libraryapp.util

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TxHelper {

    @Transactional
    fun exec(block: () -> Unit) {
        block()
    }

}