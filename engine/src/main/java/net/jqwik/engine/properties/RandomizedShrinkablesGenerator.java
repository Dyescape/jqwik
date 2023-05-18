package net.jqwik.engine.properties;

import net.jqwik.api.*;
import net.jqwik.api.parameters.ParameterSet;
import net.jqwik.api.support.CollectorsSupport;
import net.jqwik.engine.SourceOfRandomness;
import net.jqwik.engine.execution.DynamicInfo;
import net.jqwik.engine.execution.GenerationInfo;
import net.jqwik.engine.properties.arbitraries.EdgeCasesSupport;
import net.jqwik.engine.support.MethodParameter;
import net.jqwik.engine.support.random.BranchingRandom;
import net.jqwik.engine.support.types.TypeUsageImpl;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.Math.max;

public class RandomizedShrinkablesGenerator implements ForAllParametersGenerator {
	private static final Logger LOG = Logger.getLogger(RandomizedShrinkablesGenerator.class.getName());

	public static RandomizedShrinkablesGenerator forParameters(
			List<MethodParameter> parameters,
			ArbitraryResolver arbitraryResolver,
			Random random,
			int genSize,
			EdgeCasesMode edgeCasesMode
	) {
		List<EdgeCases<Object>> listOfEdgeCases = listOfEdgeCases(parameters, arbitraryResolver, edgeCasesMode, genSize);
		int edgeCasesTotal = calculateEdgeCasesTotal(listOfEdgeCases);

		logEdgeCasesOutnumberTriesIfApplicable(genSize, edgeCasesTotal);

		return new RandomizedShrinkablesGenerator(
				randomShrinkablesGenerator(
						parameters,
						arbitraryResolver,
						genSize,
						edgeCasesMode.activated()
				),
				new EdgeCasesGenerator(ParameterSet.direct(listOfEdgeCases)),
				edgeCasesMode,
				genSize,
				edgeCasesMode.activated(),
				edgeCasesTotal,
				calculateBaseToEdgeCaseRatio(listOfEdgeCases, genSize),
				random.nextLong()
		);
	}

	private static void logEdgeCasesOutnumberTriesIfApplicable(int genSize, int edgeCasesTotal) {
		int logEdgeCasesExceedTriesLimit = max(genSize, 100);
		if (edgeCasesTotal >= logEdgeCasesExceedTriesLimit && genSize > 1) {
			String message = String.format(
					"Edge case generation exceeds number of tries. Stopped after %s generated cases.",
					edgeCasesTotal
			);
			LOG.log(Level.INFO, message);
		}
	}

	private static int calculateEdgeCasesTotal(final List<EdgeCases<Object>> listOfEdgeCases) {
		if (listOfEdgeCases.isEmpty()) {
			return 0;
		}
		return listOfEdgeCases.stream().mapToInt(EdgeCases::size).reduce(1, (a, b) -> a * b);
	}

	private static PurelyRandomShrinkablesGenerator randomShrinkablesGenerator(
			List<MethodParameter> parameters,
			ArbitraryResolver arbitraryResolver,
			int genSize,
			boolean withEdgeCases
	) {
		List<RandomizedParameterGenerator> parameterGenerators = parameterGenerators(parameters, arbitraryResolver, genSize, withEdgeCases);
		return new PurelyRandomShrinkablesGenerator(ParameterSet.direct(parameterGenerators), genSize, withEdgeCases);
	}

	private static List<RandomizedParameterGenerator> parameterGenerators(
			List<MethodParameter> parameters,
			ArbitraryResolver arbitraryResolver,
			int genSize,
			boolean withEdgeCases
	) {
		return parameters.stream()
				.map(parameter -> resolveParameter(arbitraryResolver, parameter, genSize, withEdgeCases))
				.collect(Collectors.toList());
	}

	private static List<EdgeCases<Object>> listOfEdgeCases(
			List<MethodParameter> parameters,
			ArbitraryResolver arbitraryResolver,
			EdgeCasesMode edgeCasesMode,
			int genSize
	) {
		List<EdgeCases<Object>> listOfEdgeCases = new ArrayList<>();

		if (edgeCasesMode.activated() && !parameters.isEmpty()) {
			int maxEdgeCasesNextParameter = genSize;
			for (MethodParameter parameter : parameters) {
				EdgeCases<Object> edgeCases = resolveEdgeCases(arbitraryResolver, parameter, maxEdgeCasesNextParameter);
				// If a single parameter has no edge cases the combination of parameters have no edge cases
				if (edgeCases.isEmpty()) {
					return Collections.emptyList();
				}
				listOfEdgeCases.add(edgeCases);
				maxEdgeCasesNextParameter = calculateNextParamMaxEdgeCases(maxEdgeCasesNextParameter, edgeCases.size());
			}
		}
		return listOfEdgeCases;
	}

	private static int calculateNextParamMaxEdgeCases(int maxEdgeCases, int baseCasesSize) {
		int maxDerivedEdgeCases = Math.max(1, maxEdgeCases / baseCasesSize);
		// When in doubt generate a few more edge cases
		if (maxEdgeCases % baseCasesSize > 0) {
			maxDerivedEdgeCases += 1;
		}
		return maxDerivedEdgeCases;
	}

	private static int calculateBaseToEdgeCaseRatio(List<EdgeCases<Object>> edgeCases, int genSize) {
		int countEdgeCases = edgeCases.stream().mapToInt(EdgeCases::size).reduce(1, (a, b) -> max(a * b, 1));
		return EdgeCasesGenerator.calculateBaseToEdgeCaseRatio(genSize, countEdgeCases);
	}

	private static EdgeCases<Object> resolveEdgeCases(
			ArbitraryResolver arbitraryResolver,
			MethodParameter parameter,
			int maxEdgeCases
	) {
		List<EdgeCases<Object>> edgeCases = resolveArbitraries(arbitraryResolver, parameter)
				.stream()
				.map(objectArbitrary -> objectArbitrary.edgeCases(maxEdgeCases))
				.collect(Collectors.toList());
		return EdgeCasesSupport.concat(edgeCases, maxEdgeCases);
	}

	private static RandomizedDirectParameterGenerator resolveParameter(
			ArbitraryResolver arbitraryResolver,
			MethodParameter parameter,
			int genSize,
			boolean withEdgeCases
	) {
		Set<Arbitrary<Object>> arbitraries = resolveArbitraries(arbitraryResolver, parameter);

		// This logging only makes sense if arbitraries get the capability to describe themselves
		// Supplier<String> message = () -> String.format("Parameter %s generated by arbitraries %s", parameter.getRawParameter(), arbitraries);
		// LOG.log(Level.INFO, message);

		return new RandomizedDirectParameterGenerator(parameter, arbitraries, genSize, withEdgeCases);
	}

	private static Set<Arbitrary<Object>> resolveArbitraries(ArbitraryResolver arbitraryResolver, MethodParameter parameter) {
		Set<Arbitrary<Object>> arbitraries =
				arbitraryResolver.forParameter(parameter).stream()
						.map(Arbitrary::asGeneric)
						.collect(CollectorsSupport.toLinkedHashSet());
		if (arbitraries.isEmpty()) {
			throw new CannotFindArbitraryException(TypeUsageImpl.forParameter(parameter), parameter.getAnnotation(ForAll.class));
		}
		return arbitraries;
	}

	private final PurelyRandomShrinkablesGenerator randomGenerator;
	private final EdgeCasesGenerator edgeCasesGenerator;
	private final EdgeCasesMode edgeCasesMode;
	private int edgeCasesTotal;
	private int baseToEdgeCaseRatio;
	private final long baseRandomSeed;
	private final BranchingRandom random;
	private final Random edgeCaseDeciderRandom;
	private final Random baseGeneratorRandom;
	private final Random dynamicSeedGeneratorRandom;
	private final int genSize;
	private final boolean withEdgeCases;

	private boolean allEdgeCasesGenerated = false;
	private boolean edgeCaseJustGenerated = false;
	private boolean normalCaseJustGenerated = false;
	private int normalCasesTried = -1;
	private int edgeCasesTried = 0;
	private Map<String, Integer> dynamicIndices = new HashMap<>();

	private RandomizedShrinkablesGenerator(
			PurelyRandomShrinkablesGenerator randomGenerator,
			EdgeCasesGenerator edgeCasesGenerator,
			EdgeCasesMode edgeCasesMode,
			int genSize,
			boolean withEdgeCases,
			int edgeCasesTotal,
			int baseToEdgeCaseRatio,
			long baseRandomSeed
	) {
		this.randomGenerator = randomGenerator;
		this.edgeCasesGenerator = edgeCasesGenerator;
		this.edgeCasesMode = edgeCasesMode;
		this.genSize = genSize;
		this.withEdgeCases = withEdgeCases;
		this.edgeCasesTotal = edgeCasesTotal;
		this.baseToEdgeCaseRatio = baseToEdgeCaseRatio;
		this.baseRandomSeed = baseRandomSeed;
		this.random = new BranchingRandom(baseRandomSeed);

		this.edgeCaseDeciderRandom = random.branch();
		this.baseGeneratorRandom = random.branch();
		this.dynamicSeedGeneratorRandom = random.branch();
	}

	@Override
	public boolean hasNext() {
		// Randomized generation should always be able to generate a next set of values
		return true;
	}

	@Override
	public ParameterSet<Shrinkable<Object>> next() {
		if (!allEdgeCasesGenerated) {
			if (edgeCasesMode.generateFirst()) {
				if (edgeCasesGenerator.hasNext()) {
					edgeCasesTried++;
					normalCaseJustGenerated = false;
					edgeCaseJustGenerated = true;
					return edgeCasesGenerator.next();
				} else {
					allEdgeCasesGenerated = true;
				}
			}
			if (edgeCasesMode.mixIn()) {
				if (shouldGenerateEdgeCase(edgeCaseDeciderRandom)) {
					if (edgeCasesGenerator.hasNext()) {
						edgeCasesTried++;
						normalCaseJustGenerated = false;
						edgeCaseJustGenerated = true;
						return edgeCasesGenerator.next();
					} else {
						allEdgeCasesGenerated = true;
					}
				}
			}
		}

		normalCasesTried++;

		edgeCaseJustGenerated = false;
		normalCaseJustGenerated = true;

		return randomGenerator.generateNext(baseGeneratorRandom);
	}

	@Override
	public ParameterSet<Shrinkable<Object>> peek(GenerationInfo info) {
		ParameterSet<Shrinkable<Object>> result = null;

		if (info.edgeCase()) {
			for (int i = 0; i <= info.baseGenerationIndex(); i++) {
				result = edgeCasesGenerator.next();
			}
		} else {
			BranchingRandom random = new BranchingRandom(baseRandomSeed);
			random.branch(); // Edge case decider
			Random baseGeneratorRandom = random.branch();

			for (int i = 0; i <= info.baseGenerationIndex(); i++) {
				result = randomGenerator.generateNext(baseGeneratorRandom);
			}
		}

		return result;
	}

	@Override
	public int baseGenerationIndex() {
		return edgeCaseJustGenerated
				? edgeCasesGenerator.directIteration()
				: normalCasesTried;
	}

	@Override
	public Map<String, Integer> dynamicProgress() {
		return edgeCaseJustGenerated
				? edgeCasesGenerator.dynamicIterations()
				: normalDynamicProgress();
	}

	@Override
	public boolean edgeCase() {
		return edgeCaseJustGenerated;
	}

	private Map<String, Integer> normalDynamicProgress() {
		Map<String, Integer> result = new HashMap<>();

		for (Map.Entry<String, Integer> entry : dynamicIndices.entrySet()) {
			result.put(entry.getKey(), normalCasesTried - entry.getValue());
		}

		return result;
	}

	@Override
	public int edgeCasesTotal() {
		return edgeCasesTotal;
	}

	@Override
	public int edgeCasesTried() {
		return edgeCasesTried;
	}

	private boolean shouldGenerateEdgeCase(Random localRandom) {
		return localRandom.nextInt(baseToEdgeCaseRatio + 1) == 0;
	}

	@Override
	public Shrinkable<Object> peekDynamicParameter(String name,
												   Arbitrary<Object> arbitrary,
												   DynamicInfo info,
												   boolean edgeCase) {
		Shrinkable<Object> result = null;

		if (edgeCase) {
			EdgeCases<Object> edgeCases = arbitrary.edgeCases(info.progress() + 1);
			Iterator<Shrinkable<Object>> iterator = edgeCases.iterator();

			for (int i = 0; i <= info.progress(); i++) {
				result = iterator.next();
			}
		} else {
			BranchingRandom random = new BranchingRandom(baseRandomSeed);
			random.branch(); // Edge case decider
			random.branch(); // Base generator
			Random dynamicSeedGenerator = random.branch(); // Dynamic seed generator
			long seed = 0;
			for (int i = 0; i <= info.introductionIndex(); i++) {
				seed = dynamicSeedGenerator.nextLong();
			}

			Random dynamicRandom = SourceOfRandomness.newRandom(seed);
			RandomGenerator<Object> generator = arbitrary.generator(genSize, withEdgeCases);

			for (int i = 0; i <= info.progress(); i++) {
				result = generator.next(dynamicRandom);
			}
		}

		return result;
	}

	@Override
	public Shrinkable<Object> registerDynamicParameter(String name, Arbitrary<Object> arbitrary) {
		dynamicIndices.put(name, normalCaseJustGenerated ? normalCasesTried : normalCasesTried + 1);

		int triesRemaining = genSize - normalCasesTried;
		int edgeCasesRemaining = Math.max(edgeCasesTotal - edgeCasesTried, 1);
		int maxEdgeCases = calculateNextParamMaxEdgeCases(triesRemaining, edgeCasesRemaining);

		EdgeCases<Object> edgeCases = arbitrary.edgeCases(maxEdgeCases);

		if (!edgeCases.isEmpty()) {
			allEdgeCasesGenerated = false;
		}

		Random random = SourceOfRandomness.newRandom(dynamicSeedGeneratorRandom.nextLong());

		Shrinkable<Object> edgeCase = edgeCasesGenerator.pushDynamic(name, edgeCases, edgeCaseJustGenerated);
		Shrinkable<Object> randomValue = randomGenerator.pushDynamic(name, arbitrary, random, normalCaseJustGenerated);

		return edgeCaseJustGenerated ? edgeCase : normalCaseJustGenerated ? randomValue : null;
	}
}
