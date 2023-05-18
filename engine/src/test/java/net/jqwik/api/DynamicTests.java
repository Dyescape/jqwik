package net.jqwik.api;

import net.jqwik.api.dynamic.Dynamic;
import net.jqwik.api.dynamic.MissingDynamicContextException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;

public class DynamicTests {
    @Test // Normal test, because a jqwik test will have the dynamic context
    void throwsOnMissingContextWithType() {
        Assertions.assertThatThrownBy(() -> Dynamic.parameter("name", Arbitraries::strings))
                .isInstanceOf(MissingDynamicContextException.class);
    }

    @Test // Normal test, because a jqwik test will have the dynamic context
    void throwsOnMissingContextWithSupplier() {
        Assertions.assertThatThrownBy(() -> Dynamic.parameter("name", String.class))
                .isInstanceOf(MissingDynamicContextException.class);
    }
}
