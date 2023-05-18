package net.jqwik.engine.dynamic;

import net.jqwik.api.Arbitrary;

import java.util.function.Supplier;

public interface DynamicContext {
    <V> V parameter(String name, Supplier<Arbitrary<V>> provider);
}
