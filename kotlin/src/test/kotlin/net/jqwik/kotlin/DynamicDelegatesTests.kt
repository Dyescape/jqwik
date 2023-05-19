package net.jqwik.kotlin

import net.jqwik.api.*
import net.jqwik.kotlin.api.arbitrary
import org.assertj.core.api.Assertions.assertThat

class DynamicDelegatesTests {

    @Property(generation = GenerationMode.RANDOMIZED)
    @Report(Reporting.GENERATED)
    fun canUseDynamicDelegate() {
        val string by arbitrary { Arbitraries.strings().map { it + "Test" } }

        assertThat(string).endsWith("Test")
    }
}
