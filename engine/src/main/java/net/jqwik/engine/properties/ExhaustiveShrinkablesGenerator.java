package net.jqwik.engine.properties;

import java.util.*;
import java.util.stream.*;

import net.jqwik.api.*;
import net.jqwik.api.parameters.ParameterSet;
import net.jqwik.engine.execution.DynamicInfo;
import net.jqwik.engine.execution.GenerationInfo;
import net.jqwik.engine.support.*;
import net.jqwik.engine.support.combinatorics.CombinedParameterIterator;
import net.jqwik.engine.support.types.*;

public class ExhaustiveShrinkablesGenerator implements ForAllParametersGenerator {
	public static ExhaustiveShrinkablesGenerator forParameters(
		List<MethodParameter> parameters,
		ArbitraryResolver arbitraryResolver,
		long maxNumberOfSamples
	) {
		List<List<ExhaustiveGenerator<Object>>> exhaustiveGenerators =
			parameters.stream()
					  .map(parameter -> resolveParameter(arbitraryResolver, parameter, maxNumberOfSamples))
					  .collect(Collectors.toList());

		return new ExhaustiveShrinkablesGenerator(ParameterSet.direct(exhaustiveGenerators), maxNumberOfSamples);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static List<ExhaustiveGenerator<Object>> resolveParameter(
		ArbitraryResolver arbitraryResolver,
		MethodParameter parameter,
		long maxNumberOfSamples
	) {
		Set<Arbitrary<?>> arbitraries = arbitraryResolver.forParameter(parameter);
		if (arbitraries.isEmpty()) {
			throw new CannotFindArbitraryException(TypeUsageImpl.forParameter(parameter), parameter.getAnnotation(ForAll.class));
		}

		List<ExhaustiveGenerator<Object>> exhaustiveGenerators = new ArrayList<>();
		for (Arbitrary arbitrary : arbitraries) {
			exhaustiveGenerators.add(generatorFor(arbitrary, maxNumberOfSamples));
		}
		return exhaustiveGenerators;

	}

	private static ExhaustiveGenerator<Object> generatorFor(Arbitrary<Object> arbitrary, long maxNumberOfSamples) {
		Optional<ExhaustiveGenerator<Object>> optionalGenerator = arbitrary.exhaustive(maxNumberOfSamples);

		if (!optionalGenerator.isPresent()) {
			String message = String.format("Arbitrary %s does not provide exhaustive generator", arbitrary);
			throw new JqwikException(message);
		}

		return optionalGenerator.get();
	}

	private final ParameterSet<List<ExhaustiveGenerator<Object>>> generators;
	private long maxCount;
	private CombinedParameterIterator<Shrinkable<Object>> combinatorialIterator;
	private final long maxNumberOfSamples;
	private boolean hasGenerated = false;

	private ExhaustiveShrinkablesGenerator(
			ParameterSet<List<ExhaustiveGenerator<Object>>> generators,
			long maxNumberOfSamples
	) {
		this.maxCount = generators.all()
							.stream()
							.mapToLong(set -> set.stream().mapToLong(ExhaustiveGenerator::maxCount).sum())
							.reduce((product, count) -> product * count)
							.orElse(1L);
		this.generators = generators;
		this.maxNumberOfSamples = maxNumberOfSamples;
		this.combinatorialIterator = combine(generators);
	}

	private CombinedParameterIterator<Shrinkable<Object>> combine(
			ParameterSet<List<ExhaustiveGenerator<Object>>> generators
	) {
		ParameterSet<Iterable<Shrinkable<Object>>> iterables = generators.map(this::concat).map(this::unshrinkable);

		return Combinatorics.combineParameters(iterables);
	}

	private Iterable<Object> concat(List<ExhaustiveGenerator<Object>> generatorList) {
		List<Iterable<Object>> iterables = generatorList
											   .stream()
											   .map(g -> (Iterable<Object>) g)
											   .collect(Collectors.toList());
		return () -> Combinatorics.concat(iterables);
	}

	private Iterable<Shrinkable<Object>> unshrinkable(Iterable<Object> generators) {
		return () -> {
			Iterator<Object> iterator = generators.iterator();

			return new Iterator<Shrinkable<Object>>() {
				@Override
				public boolean hasNext() {
					return iterator.hasNext();
				}

				@Override
				public Shrinkable<Object> next() {
					return Shrinkable.unshrinkable(iterator.next());
				}
			};
		};
	}

	@Override
	public boolean hasNext() {
		return combinatorialIterator.hasNext();
	}

	@Override
	public ParameterSet<Shrinkable<Object>> next() {
		hasGenerated = true;

		return combinatorialIterator.next();
	}

	@Override
	public ParameterSet<Shrinkable<Object>> peek(GenerationInfo info) {
		CombinedParameterIterator<Shrinkable<Object>> iterator = combine(generators);

		for (int i = 0; i < info.baseGenerationIndex(); i++) {
			iterator.next();
		}

		return iterator.next();
	}

	@Override
	public int baseGenerationIndex() {
		return combinatorialIterator.directIteration();
	}

	@Override
	public Map<String, Integer> dynamicProgress() {
		return combinatorialIterator.dynamicIterations();
	}

	@Override
	public long requiredTries() {
		return maxCount;
	}

	@Override
	public Shrinkable<Object> peekDynamicParameter(String name,
												   Arbitrary<Object> arbitrary,
												   DynamicInfo info,
												   boolean edgeCase) {
		if (edgeCase) {
			throw new JqwikException("Exhaustive generator does not support explicit edge cases");
		}

		ExhaustiveGenerator<Object> generator = generatorFor(arbitrary, maxNumberOfSamples);
		Iterator<Object> iterator = generator.iterator();

		if (!iterator.hasNext()) {
			throw new NoSuchElementException();
		}

		for (int i = 0; i < info.progress(); i++) {
			iterator.next();

			if (!iterator.hasNext()) {
				iterator = generator.iterator();
			}
		}

		return Shrinkable.unshrinkable(iterator.next());
	}

	@Override
	public Shrinkable<Object> registerDynamicParameter(String name, Arbitrary<Object> arbitrary) {
		ExhaustiveGenerator<Object> generator = generatorFor(arbitrary, maxNumberOfSamples);

		maxCount *= generator.maxCount();

		return combinatorialIterator.pushDynamic(name, unshrinkable(generator), hasGenerated);
	}
}
