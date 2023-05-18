package net.jqwik.engine.execution;

import java.math.*;
import java.util.*;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;
import net.jqwik.api.parameters.ParameterSet;
import net.jqwik.engine.properties.*;
import net.jqwik.engine.properties.shrinking.*;

public class ParametersGeneratorForTests implements ParametersGenerator {
	int index = 0;

	@Override
	public boolean hasNext() {
		return index < 200;
	}

	@Override
	public ParameterSet<Shrinkable<Object>> next(TryLifecycleContext context) {
		return ParameterSet.direct(Arrays.asList(shrinkableInt(++index)));
	}

	@Override
	public ParameterSet<Shrinkable<Object>> peek(GenerationInfo info, TryLifecycleContext context) {
		if (info.generationIndex() >= 200) return null;

		return ParameterSet.direct(Arrays.asList(shrinkableInt(info.generationIndex())));
	}

	private Shrinkable<Object> shrinkableInt(int anInt) {
		Range<BigInteger> range = Range.of(BigInteger.ZERO, BigInteger.valueOf(1000));
		BigInteger value = BigInteger.valueOf(anInt);
		return new ShrinkableBigInteger(value, range, BigInteger.ZERO)
			.map(BigInteger::intValueExact)
			.asGeneric();
	}

	@Override
	public int edgeCasesTotal() {
		return 0;
	}

	@Override
	public int edgeCasesTried() {
		return 0;
	}

	@Override
	public GenerationInfo generationInfo(String randomSeed) {
		return new GenerationInfo(randomSeed, index, index, false, Collections.emptyMap());
	}

	@Override
	public <V> V registerDynamicParameter(String name, Arbitrary<V> arbitrary) {
		throw new UnsupportedOperationException("Cannot add dynamic parameters");
	}
}
