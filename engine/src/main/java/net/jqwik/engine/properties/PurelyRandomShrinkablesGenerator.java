package net.jqwik.engine.properties;

import java.util.*;

import net.jqwik.api.*;
import net.jqwik.api.parameters.ParameterSet;
import net.jqwik.api.providers.*;

class PurelyRandomShrinkablesGenerator {

	private final ParameterSet<RandomizedParameterGenerator> parameterGenerators;

	PurelyRandomShrinkablesGenerator(ParameterSet<RandomizedParameterGenerator> parameterGenerators) {
		this.parameterGenerators = parameterGenerators;
	}

	ParameterSet<Shrinkable<Object>> generateNext(Random random) {
		Map<TypeUsage, Arbitrary<Object>> generatorsCache = new LinkedHashMap<>();
		return parameterGenerators.map(generator -> generator.next(random, generatorsCache));
	}

}
