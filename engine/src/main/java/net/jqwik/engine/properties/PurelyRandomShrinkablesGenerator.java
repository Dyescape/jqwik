package net.jqwik.engine.properties;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.Shrinkable;
import net.jqwik.api.parameters.ParameterSet;
import net.jqwik.api.providers.TypeUsage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

class PurelyRandomShrinkablesGenerator {
    private final ParameterSet<RandomizedParameterGenerator> parameterGenerators;
    private final int genSize;
    private final boolean withEdgeCases;
    private Random lastRandom;

    PurelyRandomShrinkablesGenerator(
            ParameterSet<RandomizedParameterGenerator> parameterGenerators,
            int genSize,
            boolean withEdgeCases
    ) {
        this.parameterGenerators = parameterGenerators;
        this.genSize = genSize;
        this.withEdgeCases = withEdgeCases;
    }

    ParameterSet<Shrinkable<Object>> generateNext(Random random) {
        Map<TypeUsage, Arbitrary<Object>> generatorsCache = new LinkedHashMap<>();

        lastRandom = random;
        return parameterGenerators.map(generator -> generator.next(random, generatorsCache));
    }

    public Shrinkable<Object> pushDynamic(
            String name,
            Arbitrary<Object> arbitrary,
            Random random,
            boolean active
    ) {
        RandomizedDynamicParameterGenerator generator = new RandomizedDynamicParameterGenerator(
                arbitrary,
                random,
                genSize,
                withEdgeCases
        );

        parameterGenerators.setDynamic(name, generator);

        return active ? generator.next(lastRandom, new LinkedHashMap<>()) : null;
    }
}
