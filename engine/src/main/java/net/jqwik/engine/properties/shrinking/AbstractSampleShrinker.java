package net.jqwik.engine.properties.shrinking;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import net.jqwik.api.*;
import net.jqwik.api.Tuple.*;
import net.jqwik.api.lifecycle.*;
import net.jqwik.api.parameters.ParameterSet;
import net.jqwik.engine.properties.*;

abstract class AbstractSampleShrinker {

	private static ShrinkingDistance calculateDistance(ParameterSet<Shrinkable<Object>> shrinkables) {
		return ShrinkingDistance.forCollection(shrinkables.all());
	}

	private final Map<ParameterSet<Object>, TryExecutionResult> falsificationCache;

	public AbstractSampleShrinker(Map<ParameterSet<Object>, TryExecutionResult> falsificationCache) {
		this.falsificationCache = falsificationCache;
	}

	public abstract FalsifiedSample shrink(
		Falsifier<ParameterSet<Object>> falsifier,
		FalsifiedSample sample,
		Consumer<FalsifiedSample> shrinkSampleConsumer,
		Consumer<FalsifiedSample> shrinkAttemptConsumer
	);

	protected FalsifiedSample shrink(
		Falsifier<ParameterSet<Object>> falsifier,
		FalsifiedSample sample,
		Consumer<FalsifiedSample> sampleShrunkConsumer,
		Consumer<FalsifiedSample> shrinkAttemptConsumer,
		Function<ParameterSet<Shrinkable<Object>>, Stream<ParameterSet<Shrinkable<Object>>>> supplyShrinkCandidates
	) {
		ParameterSet<Shrinkable<Object>> currentShrinkBase = sample.shrinkables();
		Optional<FalsifiedSample> bestResult = Optional.empty();
		FilteredResults filteredResults = new FilteredResults();

		while (true) {
			ShrinkingDistance currentDistance = calculateDistance(currentShrinkBase);

			FalsifiedSample currentBest = bestResult.orElse(null);

			Optional<Tuple3<ParameterSet<Object>, ParameterSet<Shrinkable<Object>>, TryExecutionResult>> newShrinkingResult =
				supplyShrinkCandidates.apply(currentShrinkBase)
								 .peek(ignore -> shrinkAttemptConsumer.accept(currentBest))
								 .filter(shrinkables -> calculateDistance(shrinkables).compareTo(currentDistance) <= 0)
								 .map(shrinkables -> {
									 ParameterSet<Object> params = shrinkables.map(Shrinkable::value);
									 TryExecutionResult result = falsify(falsifier, params);
									 return Tuple.of(params, shrinkables, result);
								 })
								 .peek(t -> {
									 // Remember best 10 invalid results in case no  falsified shrink is found
									 if (t.get3().isInvalid() && calculateDistance(t.get2()).compareTo(currentDistance) < 0) {
										 filteredResults.push(t);
									 }
								 })
								 .filter(t -> t.get3().isFalsified())
								 .findAny();

			if (newShrinkingResult.isPresent()) {
				Tuple3<ParameterSet<Object>, ParameterSet<Shrinkable<Object>>, TryExecutionResult> falsifiedTry = newShrinkingResult.get();
				TryExecutionResult tryExecutionResult = falsifiedTry.get3();
				FalsifiedSample falsifiedSample = new FalsifiedSampleImpl(
					falsifiedTry.get1(),
					falsifiedTry.get2(),
					tryExecutionResult.throwable(),
					tryExecutionResult.footnotes()
				);
				sampleShrunkConsumer.accept(falsifiedSample);
				bestResult = Optional.of(falsifiedSample);
				currentShrinkBase = falsifiedTry.get2();
				filteredResults.clear();
			} else if (!filteredResults.isEmpty()) {
				Tuple3<ParameterSet<Object>, ParameterSet<Shrinkable<Object>>, TryExecutionResult> aFilteredResult = filteredResults.pop();
				currentShrinkBase = aFilteredResult.get2();
			} else {
				break;
			}
		}

		return bestResult.orElse(sample);
	}

	private TryExecutionResult falsify(Falsifier<ParameterSet<Object>> falsifier, ParameterSet<Object> params) {
		// I wonder in which cases this is really an optimization
		return falsificationCache.computeIfAbsent(params, p -> falsifier.execute(params));
	}

	private static class FilteredResults {

		public static final int MAX_SIZE = 100;

		Comparator<? super Tuple3<ParameterSet<Object>, ParameterSet<Shrinkable<Object>>, TryExecutionResult>> resultComparator =
			Comparator.comparing(left -> calculateDistance(left.get2()));

		PriorityQueue<Tuple3<ParameterSet<Object>, ParameterSet<Shrinkable<Object>>, TryExecutionResult>> prioritizedResults = new PriorityQueue<>(resultComparator);

		Set<Tuple3<ParameterSet<Object>, ParameterSet<Shrinkable<Object>>, TryExecutionResult>> removedResults = new LinkedHashSet<>();

		void push(Tuple3<ParameterSet<Object>, ParameterSet<Shrinkable<Object>>, TryExecutionResult> result) {
			if (removedResults.contains(result)) {
				return;
			}
			prioritizedResults.add(result);
			if (size() > MAX_SIZE) {
				prioritizedResults.poll();
			}
		}

		int size() {
			return prioritizedResults.size();
		}

		boolean isEmpty() {
			return prioritizedResults.isEmpty();
		}

		Tuple3<ParameterSet<Object>, ParameterSet<Shrinkable<Object>>, TryExecutionResult> pop() {
			Tuple3<ParameterSet<Object>, ParameterSet<Shrinkable<Object>>, TryExecutionResult> result = prioritizedResults.peek();
			prioritizedResults.remove(result);
			removedResults.add(result);
			return result;
		}

		public void clear() {
			prioritizedResults.clear();
			removedResults.clear();
		}
	}
}
