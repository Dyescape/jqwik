package net.jqwik.engine.properties;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.Shrinkable;
import net.jqwik.api.providers.TypeUsage;

import java.util.Map;
import java.util.Random;

interface RandomizedParameterGenerator {
    Shrinkable<Object> next(Random random, Map<TypeUsage, Arbitrary<Object>> arbitrariesCache);
}
