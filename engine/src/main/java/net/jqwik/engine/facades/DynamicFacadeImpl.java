package net.jqwik.engine.facades;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.dynamic.Dynamic;
import net.jqwik.engine.dynamic.CurrentDynamicContext;

import java.util.function.Supplier;

public class DynamicFacadeImpl extends Dynamic.DynamicFacade {
    @Override
    protected <V> V parameter(String name, Supplier<Arbitrary<V>> provider) {
        return CurrentDynamicContext.get().parameter(name, provider);
    }
}
