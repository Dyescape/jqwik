package net.jqwik.engine.properties;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.Shrinkable;
import net.jqwik.api.providers.TypeUsage;

import java.util.Map;
import java.util.Random;

class RandomizedDynamicParameterGenerator implements RandomizedParameterGenerator {
    private final Arbitrary<Object> arbitrary;
    private final Random random;
    private final int genSize;
    private final boolean withEdgeCases;

    RandomizedDynamicParameterGenerator(
            Arbitrary<Object> arbitrary,
            Random random,
            int genSize,
            boolean withEdgeCases
    ) {
        this.arbitrary = arbitrary;
        this.random = random;
        this.genSize = genSize;
        this.withEdgeCases = withEdgeCases;
    }

    @Override
    public Shrinkable<Object> next(Random random, Map<TypeUsage, Arbitrary<Object>> arbitrariesCache) {
        return arbitrary.generator(genSize, withEdgeCases).next(this.random);
    }
}
