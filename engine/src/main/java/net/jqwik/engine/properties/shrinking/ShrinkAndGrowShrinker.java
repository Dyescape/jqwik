package net.jqwik.engine.properties.shrinking;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;
import net.jqwik.api.parameters.ParameterReference;
import net.jqwik.api.parameters.ParameterSet;
import net.jqwik.engine.support.*;

class ShrinkAndGrowShrinker extends AbstractSampleShrinker {

	public ShrinkAndGrowShrinker(Map<ParameterSet<Object>, TryExecutionResult> falsificationCache) {
		super(falsificationCache);
	}

	@Override
	public FalsifiedSample shrink(
		Falsifier<ParameterSet<Object>> falsifier,
		FalsifiedSample sample,
		Consumer<FalsifiedSample> sampleShrunkConsumer,
		Consumer<FalsifiedSample> shrinkAttemptConsumer
	) {
		FalsifiedSample current = sample;
		List<Tuple.Tuple2<ParameterReference, ParameterReference>> allPairs =
			Combinatorics.distinctPairs(sample.shrinkables().references()).collect(Collectors.toList());
		for (Tuple.Tuple2<ParameterReference, ParameterReference> pair : allPairs) {
			current = shrinkAndGrow(falsifier, current, sampleShrunkConsumer, shrinkAttemptConsumer, pair.get1(), pair.get2());
		}
		return current;
	}

	private FalsifiedSample shrinkAndGrow(
		Falsifier<ParameterSet<Object>> falsifier,
		FalsifiedSample sample,
		Consumer<FalsifiedSample> sampleShrunkConsumer,
		Consumer<FalsifiedSample> shrinkAttemptConsumer,
		ParameterReference index1,
		ParameterReference index2
	) {
		Function<ParameterSet<Shrinkable<Object>>, Stream<ParameterSet<Shrinkable<Object>>>> shrinker =
			shrinkables -> {
				Shrinkable<Object> before = shrinkables.get(index1);
				Stream<Shrinkable<Object>> afterStream = before.shrink();
				return afterStream.flatMap(after -> {
					Optional<Shrinkable<Object>> optionalShrink2 = shrinkables.get(index2).grow(before, after);
					if (optionalShrink2.isPresent()) {
						ParameterSet<Shrinkable<Object>> newShrinkables = sample.shrinkables().copy();
						newShrinkables.set(index1, after);
						newShrinkables.set(index2, optionalShrink2.get());
						return Stream.of(newShrinkables);
					} else {
						return Stream.empty();
					}
				});
			};

		return shrink(
			falsifier,
			sample,
			sampleShrunkConsumer,
			shrinkAttemptConsumer,
			shrinker
		);
	}

}
