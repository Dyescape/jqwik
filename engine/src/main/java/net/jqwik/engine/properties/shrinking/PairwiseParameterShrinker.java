package net.jqwik.engine.properties.shrinking;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;
import net.jqwik.api.parameters.ParameterReference;
import net.jqwik.api.parameters.ParameterSet;
import net.jqwik.engine.support.*;

class PairwiseParameterShrinker extends AbstractSampleShrinker {

	public PairwiseParameterShrinker(Map<ParameterSet<Object>, TryExecutionResult> falsificationCache) {
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
			current = shrinkPair(falsifier, current, sampleShrunkConsumer, shrinkAttemptConsumer, pair.get1(), pair.get2());
		}
		return current;
	}

	private FalsifiedSample shrinkPair(
		Falsifier<ParameterSet<Object>> falsifier,
		FalsifiedSample sample,
		Consumer<FalsifiedSample> sampleShrunkConsumer,
		Consumer<FalsifiedSample> shrinkAttemptConsumer,
		ParameterReference index1,
        ParameterReference index2
	) {
		Function<ParameterSet<Shrinkable<Object>>, Stream<ParameterSet<Shrinkable<Object>>>> shrinker =
			shrinkables -> {
				Stream<Shrinkable<Object>> shrink1 = shrinkables.get(index1).shrink();
				Stream<Shrinkable<Object>> shrink2 = shrinkables.get(index2).shrink();

				return JqwikStreamSupport.zip(shrink1, shrink2, (shrinkable1, shrinkable2) -> {
					ParameterSet<Shrinkable<Object>> newShrinkables = sample.shrinkables().copy();
					index1.update(newShrinkables, shrinkable1);
					index2.update(newShrinkables, shrinkable2);
					return newShrinkables;
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
