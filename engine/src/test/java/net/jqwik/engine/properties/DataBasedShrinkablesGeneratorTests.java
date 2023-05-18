package net.jqwik.engine.properties;

import java.util.*;
import java.util.stream.*;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import net.jqwik.api.parameters.*;
import net.jqwik.engine.*;
import net.jqwik.engine.descriptor.*;
import net.jqwik.engine.execution.*;
import net.jqwik.engine.support.*;

import static org.assertj.core.api.Assertions.*;

class DataBasedShrinkablesGeneratorTests {

	@Example
	void valuesFitParameters() {
		Iterable<Tuple.Tuple2<String, Integer>> data = Table.of(Tuple.of("a", 1), Tuple.of("b", 2));
		DataBasedShrinkablesGenerator shrinkablesGenerator = generator("stringAndInt", data);

		assertThat(values(shrinkablesGenerator.next().getDirect())).containsExactly("a", 1);
		assertThat(values(shrinkablesGenerator.next().getDirect())).containsExactly("b", 2);
		assertThat(shrinkablesGenerator.hasNext()).isFalse();
	}

	@Example
	void nullValuesFitNonPrimitiveParameters() {
		Iterable<Tuple.Tuple2<String, Integer>> data = Table.of(
			Tuple.of(null, 1),
			Tuple.of("b", null),
			Tuple.of(null, null)
		);
		DataBasedShrinkablesGenerator shrinkablesGenerator = generator("stringAndInteger", data);

		assertThat(values(shrinkablesGenerator.next().getDirect())).containsExactly(null, Integer.valueOf(1));
		assertThat(values(shrinkablesGenerator.next().getDirect())).containsExactly("b", null);
		assertThat(values(shrinkablesGenerator.next().getDirect())).containsExactly(null, null);
		assertThat(shrinkablesGenerator.hasNext()).isFalse();
	}

	@Example
	void nullValuesDontFitPrimitiveParameters() {
		Iterable<Tuple.Tuple2<String, Integer>> data = Table.of(
			Tuple.of(null, null)
		);
		DataBasedShrinkablesGenerator shrinkablesGenerator = generator("stringAndInt", data);
		assertThatThrownBy(shrinkablesGenerator::next).isInstanceOf(IncompatibleDataException.class);
	}

	@Example
	void peeking() {
		Iterable<Tuple.Tuple2<String, Integer>> data = Table.of(Tuple.of("a", 1), Tuple.of("b", 2));
		DataBasedShrinkablesGenerator shrinkablesGenerator = generator("stringAndInt", data);

		List<List<Object>> values = new ArrayList<>();
		List<GenerationInfo> generationInfos = new ArrayList<>();

		for (int i = 0; i < 2; i++) {
			values.add(shrinkablesGenerator.next().map(Shrinkable::value).getDirect());
			generationInfos.add(generationInfo(i, shrinkablesGenerator));
		}

		for (int i = 0; i < 2; i++) {
			GenerationInfo info = generationInfos.get(i);
			List<Object> value = values.get(i);
			ParameterSet<Shrinkable<Object>> peek = shrinkablesGenerator.peek(info);

			assertThat(peek.map(Shrinkable::value).getDirect()).isEqualTo(value);
		}
	}

	private GenerationInfo generationInfo(
		int index,
		DataBasedShrinkablesGenerator generator
	) {
		return new GenerationInfo(
			"",
			index,
			generator.baseGenerationIndex(),
			generator.edgeCase(),
			Collections.emptyMap()
		);
	}

	@Example
	void tooManyValues() {
		Iterable<Tuple.Tuple3<String, Integer, Boolean>> data = Table.of(Tuple.of("a", 1, true), Tuple.of("b", 2, false));
		DataBasedShrinkablesGenerator shrinkablesGenerator = generator("stringAndInt", data);

		assertThatThrownBy(shrinkablesGenerator::next).isInstanceOf(IncompatibleDataException.class);
	}

	@Example
	void tooFewValues() {
		Iterable<Tuple.Tuple1<String>> data = Table.of(Tuple.of("a"), Tuple.of("b"));
		DataBasedShrinkablesGenerator shrinkablesGenerator = generator("stringAndInt", data);

		assertThatThrownBy(shrinkablesGenerator::next).isInstanceOf(IncompatibleDataException.class);
	}

	@Example
	void valueTypesDontFit() {
		Iterable<Tuple.Tuple2<String, String>> data = Table.of(Tuple.of("a", "1"), Tuple.of("b", "2"));
		DataBasedShrinkablesGenerator shrinkablesGenerator = generator("stringAndInt", data);

		assertThatThrownBy(shrinkablesGenerator::next).isInstanceOf(IncompatibleDataException.class);
	}

	private List<Object> values(List<Shrinkable<Object>> shrinkables) {
		return shrinkables.stream().map(objectShrinkable -> objectShrinkable.value()).collect(Collectors.toList());
	}

	private DataBasedShrinkablesGenerator generator(String methodName, Iterable<? extends Tuple> data) {
		PropertyMethodDescriptor methodDescriptor = createDescriptor(methodName);
		List<MethodParameter> parameters = TestHelper.getParameters(methodDescriptor);

		return new DataBasedShrinkablesGenerator(parameters, data);
	}

	private PropertyMethodDescriptor createDescriptor(String methodName) {
		return TestHelper.createPropertyMethodDescriptor(MyProperties.class, methodName, "0", 1000, 5, ShrinkingMode.FULL);
	}

	private static class MyProperties {

		public void stringAndInt(@ForAll String aString, @ForAll int anInt) {}

		public void stringAndInteger(@ForAll String aString, @ForAll Integer anInteger) {}
	}
}
