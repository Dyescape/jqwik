package net.jqwik.engine.execution;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;
import net.jqwik.api.parameters.ParameterSet;
import net.jqwik.engine.properties.shrinking.*;

public class GenerationInfo implements Serializable {

	public final static GenerationInfo NULL = new GenerationInfo(null);

	private final String randomSeed;
	private final int generationIndex;
	private final int baseGenerationIndex;
	private final boolean edgeCase;
	private final Map<String, DynamicInfo> dynamics;
	// Store ordinals instead of enum objects so that serialization
	// in jqwik.database uses less disk space
	private final List<List<Byte>> byteSequences;

	public GenerationInfo(String randomSeed, int generationIndex) {
		this(randomSeed, generationIndex, generationIndex, false, Collections.emptyMap());
	}

	public GenerationInfo(String randomSeed) {
		this(randomSeed, 0, 0, false, Collections.emptyMap());
	}

	public GenerationInfo(String randomSeed,
						  int generationIndex,
						  int baseGenerationIndex,
						  boolean edgeCase,
						  Map<String, DynamicInfo> dynamics) {
		this(randomSeed, generationIndex, baseGenerationIndex, edgeCase, dynamics, Collections.emptyList());
	}

	private GenerationInfo(String randomSeed,
						   int generationIndex,
						   int baseGenerationIndex,
						   boolean edgeCase,
						   Map<String, DynamicInfo> dynamics,
						   List<List<Byte>> byteSequences) {
		this.randomSeed = randomSeed != null ? (randomSeed.isEmpty() ? null : randomSeed) : null;
		this.generationIndex = generationIndex;
		this.edgeCase = edgeCase;
		this.baseGenerationIndex = baseGenerationIndex;
		this.dynamics = dynamics;
		this.byteSequences = byteSequences;
	}

	private List<Byte> toByteSequence(List<TryExecutionResult.Status> shrinkingSequence) {
		return shrinkingSequence.stream().map(status -> (byte) status.ordinal()).collect(Collectors.toList());
	}

	public GenerationInfo appendShrinkingSequence(List<TryExecutionResult.Status> toAppend) {
		if (toAppend.isEmpty()) {
			return this;
		}
		List<List<Byte>> newByteSequences = new ArrayList<>(byteSequences);
		newByteSequences.add(toByteSequence(toAppend));
		return new GenerationInfo(
				randomSeed,
				generationIndex,
				baseGenerationIndex,
				edgeCase,
				dynamics,
				newByteSequences
		);
	}

	public Optional<String> randomSeed() {
		return Optional.ofNullable(randomSeed);
	}

	public int generationIndex() {
		return generationIndex;
	}

	public int baseGenerationIndex() {
		return baseGenerationIndex;
	}

	public boolean edgeCase() {
		return edgeCase;
	}

	public Map<String, DynamicInfo> dynamics() {
		return dynamics;
	}

	public Optional<ParameterSet<Shrinkable<Object>>> generateOn(ParametersGenerator generator, TryLifecycleContext context) {
		ParameterSet<Shrinkable<Object>> sample = useGenerationIndex(generator, context);
		return useShrinkingSequences(sample);
	}

	private Optional<ParameterSet<Shrinkable<Object>>> useShrinkingSequences(ParameterSet<Shrinkable<Object>> sample) {
		Optional<ParameterSet<Shrinkable<Object>>> shrunkSample = Optional.ofNullable(sample);
		for (List<TryExecutionResult.Status> shrinkingSequence : shrinkingSequences()) {
			if (!shrunkSample.isPresent()) {
				break;
			}
			shrunkSample = shrink(shrunkSample.get(), shrinkingSequence);
		}
		return shrunkSample;
	}

	private Optional<ParameterSet<Shrinkable<Object>>> shrink(
		ParameterSet<Shrinkable<Object>> sample,
		List<TryExecutionResult.Status> shrinkingSequence
	) {
		ShrunkSampleRecreator recreator = new ShrunkSampleRecreator(sample);
		return recreator.recreateFrom(shrinkingSequence);
	}

	private ParameterSet<Shrinkable<Object>> useGenerationIndex(ParametersGenerator generator, TryLifecycleContext context) {
		if (generationIndex == 0) return null;

		return generator.peek(this, context);
	}

	public List<List<TryExecutionResult.Status>> shrinkingSequences() {
		return byteSequences.stream()
							.map(this::toShrinkingSequence)
							.collect(Collectors.toList());
	}

	private List<TryExecutionResult.Status> toShrinkingSequence(List<Byte> sequence) {
		return sequence.stream().map(ordinal -> TryExecutionResult.Status.values()[ordinal]).collect(Collectors.toList());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		GenerationInfo that = (GenerationInfo) o;
		if (generationIndex != that.generationIndex) return false;
		if (baseGenerationIndex != that.baseGenerationIndex) return false;
		if (edgeCase != that.edgeCase) return false;
		if (!Objects.equals(dynamics, that.dynamics)) return false;
		if (!Objects.equals(randomSeed, that.randomSeed)) return false;
		return byteSequences.equals(that.byteSequences);
	}

	@Override
	public int hashCode() {
		int result = randomSeed != null ? randomSeed.hashCode() : 0;
		result = 31 * result + generationIndex;
		result = 31 * result + baseGenerationIndex;
		result = 31 * result + Boolean.hashCode(edgeCase);
		result = 31 * result + dynamics.hashCode();
		return result;
	}

	@Override
	public String toString() {
		List<String> sizes = byteSequences.stream().map(bytes -> "size=" + bytes.size()).collect(Collectors.toList());
		Tuple.Tuple3<String, Integer, List<String>> tuple = Tuple.of(randomSeed, generationIndex, sizes);
		return String.format("GenerationInfo%s", tuple);
	}
}
