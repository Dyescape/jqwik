package net.jqwik.api.dynamic;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.FacadeLoader;
import net.jqwik.api.providers.TypeUsage;
import org.apiguardian.api.API;

import java.util.function.Supplier;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.apiguardian.api.API.Status.INTERNAL;

@API(status = EXPERIMENTAL)
public class Dynamic {
    @API(status = INTERNAL)
    public abstract static class DynamicFacade {
        protected static DynamicFacade implementation;

        static {
            implementation = FacadeLoader.load(DynamicFacade.class);
        }

        protected abstract <V> V parameter(String name, Supplier<Arbitrary<V>> provider);
    }

    public static <V> V parameter(String name, Supplier<Arbitrary<V>> provider) {
        return DynamicFacade.implementation.parameter(name, provider);
    }

    public static <V> V parameter(String name, Class<V> type) {
        return DynamicFacade.implementation.parameter(name, () -> Arbitraries.defaultFor(type));
    }

    public static <V> V parameter(String name, TypeUsage type) {
        return DynamicFacade.implementation.parameter(name, () -> Arbitraries.defaultFor(type));
    }
}
