package net.jqwik.engine.properties;

import java.util.*;

import net.jqwik.api.parameters.ParameterSet;
import net.jqwik.engine.execution.*;

import org.assertj.core.api.*;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import net.jqwik.api.domains.*;
import net.jqwik.engine.*;
import net.jqwik.engine.descriptor.*;
import net.jqwik.engine.support.*;

import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;

class ExhaustiveShrinkablesGeneratorTests {

	@Example
	void singleIntParameter() {
		ExhaustiveShrinkablesGenerator shrinkablesGenerator = createGenerator("intFrom0to5");
		assertThat(shrinkablesGenerator.requiredTries()).isEqualTo(6);

		assertThat(shrinkablesGenerator.hasNext()).isTrue();
		assertThat(shrinkablesGenerator.next().getDirect()).containsExactly(Shrinkable.unshrinkable(0));
		assertThat(shrinkablesGenerator.next().getDirect()).containsExactly(Shrinkable.unshrinkable(1));
		assertThat(shrinkablesGenerator.next().getDirect()).containsExactly(Shrinkable.unshrinkable(2));
		assertThat(shrinkablesGenerator.next().getDirect()).containsExactly(Shrinkable.unshrinkable(3));
		assertThat(shrinkablesGenerator.next().getDirect()).containsExactly(Shrinkable.unshrinkable(4));
		assertThat(shrinkablesGenerator.hasNext()).isTrue();
		assertThat(shrinkablesGenerator.next().getDirect()).containsExactly(Shrinkable.unshrinkable(5));
		assertThat(shrinkablesGenerator.hasNext()).isFalse();
	}

	@Example
	void peeking() {
		ExhaustiveShrinkablesGenerator shrinkablesGenerator = createGenerator("intFrom0to5");

		List<List<Object>> values = new ArrayList<>();
		List<GenerationInfo> generationInfos = new ArrayList<>();

		int index = 0;
		while (shrinkablesGenerator.hasNext()) {
			values.add(shrinkablesGenerator.next().map(Shrinkable::value).getDirect());
			generationInfos.add(generationInfo(index++, shrinkablesGenerator));
		}

		for (int i = 0; i < index; i++) {
			GenerationInfo info = generationInfos.get(i);
			List<Object> value = values.get(i);
			ParameterSet<Shrinkable<Object>> peek = shrinkablesGenerator.peek(info);

			assertThat(peek.map(Shrinkable::value).getDirect()).isEqualTo(value);
		}
	}

	private GenerationInfo generationInfo(
		int index,
		ExhaustiveShrinkablesGenerator generator
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
	@Label("ambiguous Arbitrary resolution generates sum of arbitraries")
	void ambiguousArbitraryResolution() {
		ExhaustiveShrinkablesGenerator shrinkablesGenerator = createGenerator("iterables");
		assertThat(shrinkablesGenerator.requiredTries()).isEqualTo(5);

		assertThat(shrinkablesGenerator).toIterable().containsOnly(
			ParameterSet.direct(asList(Shrinkable.unshrinkable(asList(0, 0)))),
			ParameterSet.direct(asList(Shrinkable.unshrinkable(asList(0, 1)))),
			ParameterSet.direct(asList(Shrinkable.unshrinkable(asList(1, 0)))),
			ParameterSet.direct(asList(Shrinkable.unshrinkable(asList(1, 1)))),
			ParameterSet.direct(asList(Shrinkable.unshrinkable(new HashSet<>(asList(0, 1)))))
		);
	}

	@Example
	void twoIntParameters() {
		ExhaustiveShrinkablesGenerator shrinkablesGenerator = createGenerator("intFrom1to3And4to5");
		assertThat(shrinkablesGenerator.requiredTries()).isEqualTo(6);

		assertThat(shrinkablesGenerator.next().getDirect()).containsExactly(Shrinkable.unshrinkable(1), Shrinkable.unshrinkable(4));
		assertThat(shrinkablesGenerator.next().getDirect()).containsExactly(Shrinkable.unshrinkable(1), Shrinkable.unshrinkable(5));
		assertThat(shrinkablesGenerator.next().getDirect()).containsExactly(Shrinkable.unshrinkable(2), Shrinkable.unshrinkable(4));
		assertThat(shrinkablesGenerator.next().getDirect()).containsExactly(Shrinkable.unshrinkable(2), Shrinkable.unshrinkable(5));
		assertThat(shrinkablesGenerator.next().getDirect()).containsExactly(Shrinkable.unshrinkable(3), Shrinkable.unshrinkable(4));
		assertThat(shrinkablesGenerator.next().getDirect()).containsExactly(Shrinkable.unshrinkable(3), Shrinkable.unshrinkable(5));
		assertThat(shrinkablesGenerator.hasNext()).isFalse();
	}

	@Example
	void noExhaustiveGenerator() {
		Assertions.assertThatThrownBy(() -> createGenerator("doubles")).isInstanceOf(JqwikException.class);
	}

	private ExhaustiveShrinkablesGenerator createGenerator(String methodName) {
		PropertyMethodArbitraryResolver arbitraryResolver = new PropertyMethodArbitraryResolver(
			new MyProperties(),
			DomainContext.global()
		);
		return createGenerator(methodName, arbitraryResolver);
	}

	private ExhaustiveShrinkablesGenerator createGenerator(String methodName, ArbitraryResolver arbitraryResolver) {
		PropertyMethodDescriptor methodDescriptor = createDescriptor(methodName);
		List<MethodParameter> parameters = TestHelper.getParameters(methodDescriptor);

		return ExhaustiveShrinkablesGenerator.forParameters(parameters, arbitraryResolver, ExhaustiveGenerator.MAXIMUM_SAMPLES_TO_GENERATE);
	}

	private PropertyMethodDescriptor createDescriptor(String methodName) {
		return TestHelper.createPropertyMethodDescriptor(MyProperties.class, methodName, "0", 1000, 5, ShrinkingMode.FULL);
	}

	private static class MyProperties {

		public void intFrom0to5(@ForAll @IntRange(min = 0, max = 5) int anInt) {}

		public void intFrom1to3And4to5(
			@ForAll @IntRange(min = 1, max = 3) int int1,
			@ForAll @IntRange(min = 4, max = 5) int int2
		) {}

		public void iterables(@ForAll @Size(2) Iterable<@IntRange(min = 0, max = 1) Integer> iterable) {}

		public void doubles(@ForAll double aDouble) {}
	}
}
