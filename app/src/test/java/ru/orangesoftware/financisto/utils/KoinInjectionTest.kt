package ru.orangesoftware.financisto.utils

import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify
import ru.orangesoftware.financisto.app.modules

class KoinInjectionTest {

        @OptIn(KoinExperimentalAPI::class)
        @Test
        fun checkKoinModule() {

            // Verify Koin configuration
            modules.verify(extraTypes = listOf(android.content.Context::class))
        }
}
