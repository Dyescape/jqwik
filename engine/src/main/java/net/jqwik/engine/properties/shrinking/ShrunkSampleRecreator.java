package net.jqwik.engine.properties.shrinking;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;
import net.jqwik.api.parameters.ParameterSet;
import net.jqwik.engine.properties.*;

public class ShrunkSampleRecreator {

	private final ParameterSet<Shrinkable<Object>> shrinkables;

	public ShrunkSampleRecreator(ParameterSet<Shrinkable<Object>> shrinkables) {
		this.shrinkables = shrinkables;
	}

	public Optional<ParameterSet<Shrinkable<Object>>> recreateFrom(List<TryExecutionResult.Status> shrinkingSequence) {
		List<TryExecutionResult.Status> recreatingSequence = new ArrayList<>(shrinkingSequence);
		Falsifier<ParameterSet<Object>> recreatingFalsifier = falsifier(recreatingSequence);

		FalsifiedSample originalSample = createFalsifiedSample();

		AtomicInteger shrinkingSteps = new AtomicInteger(0);
		FalsifiedSample[] currentBest = new FalsifiedSample[]{originalSample};
		Consumer<FalsifiedSample> sampleShrunkConsumer = shrunkSample -> {
			// Remember current best because shrinking can be interrupted with RecreationDone exception
			currentBest[0] = shrunkSample;
			shrinkingSteps.incrementAndGet();
		};
		ShrinkingAlgorithm plainShrinker = new ShrinkingAlgorithm(
			originalSample,
			sampleShrunkConsumer,
			ignore -> {}
		);

		try {
			// Shrunk falsified sample has already been grabbed in sampleShrunkConsumer
			FalsifiedSample ignore = plainShrinker.shrink(recreatingFalsifier);
		} catch (RecreationDone ignore) {}

		if (recreatingSequence.isEmpty()) {
			return Optional.of(currentBest[0].shrinkables());
		} else {
			return Optional.empty();
		}
	}

	private FalsifiedSample createFalsifiedSample() {
		return new FalsifiedSampleImpl(
			shrinkables.map(Shrinkable::value),
			shrinkables,
			null,
			Collections.emptyList()
		);
	}

	private Falsifier<ParameterSet<Object>> falsifier(List<TryExecutionResult.Status> recreatingSequence) {
		return ignore -> {
			if (!recreatingSequence.isEmpty()) {
				TryExecutionResult.Status next = recreatingSequence.remove(0);
				switch (next) {
					case SATISFIED:
						return TryExecutionResult.satisfied();
					case INVALID:
						return TryExecutionResult.invalid();
					case FALSIFIED:
						return TryExecutionResult.falsified(null);
				}
			}
			throw new RecreationDone();
		};
	}

	private static class RecreationDone extends RuntimeException {}
}
