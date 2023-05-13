package net.jqwik.engine.properties;

import java.lang.reflect.*;
import java.util.*;

import net.jqwik.api.*;
import net.jqwik.api.domains.*;
import net.jqwik.api.parameters.ParameterSet;
import net.jqwik.engine.*;
import net.jqwik.engine.descriptor.*;
import net.jqwik.engine.support.*;

import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;

class RandomizedShrinkablesGeneratorTests {

	@Example
	void useSimpleRegisteredArbitraryProviders(@ForAll Random random) {
		RandomizedShrinkablesGenerator shrinkablesGenerator = createGenerator(random, "simpleParameters");
		ParameterSet<Shrinkable<Object>> shrinkables = shrinkablesGenerator.next();

		assertThat(shrinkables.get(0).value()).isInstanceOf(String.class);
		assertThat(shrinkables.get(1).value()).isInstanceOf(Integer.class);
	}

	@Example
	void resetting(@ForAll Random random) {
		RandomizedShrinkablesGenerator shrinkablesGenerator = createGenerator(random, "simpleParameters");

		List<Object> values1 = shrinkablesGenerator.next().map(Shrinkable::value).getDirect();
		List<Object> values2 = shrinkablesGenerator.next().map(Shrinkable::value).getDirect();
		List<Object> values3 = shrinkablesGenerator.next().map(Shrinkable::value).getDirect();

		shrinkablesGenerator.reset();
		assertThat(shrinkablesGenerator.next().map(Shrinkable::value).getDirect()).isEqualTo(values1);
		assertThat(shrinkablesGenerator.next().map(Shrinkable::value).getDirect()).isEqualTo(values2);
		assertThat(shrinkablesGenerator.next().map(Shrinkable::value).getDirect()).isEqualTo(values3);
	}

	@Example
	void severalFittingArbitraries(@ForAll Random random) {

		ArbitraryResolver arbitraryResolver = new ArbitraryResolver() {
			@Override
			public Set<Arbitrary<?>> forParameter(MethodParameter parameter) {
				Set<Arbitrary<?>> arbitraries = new HashSet<>();
				if (parameter.getType().equals(String.class)) {
					arbitraries.add(Arbitraries.just("a"));
					arbitraries.add(Arbitraries.just("b"));
				}
				if (parameter.getType().equals(int.class)) {
					arbitraries.add(Arbitraries.just(1));
					arbitraries.add(Arbitraries.just(2));
					arbitraries.add(Arbitraries.just(3));
				}
				return arbitraries;
			}
		};

		RandomizedShrinkablesGenerator shrinkablesGenerator = createGenerator(random, "simpleParameters", arbitraryResolver);

		assertAtLeastOneGenerated(shrinkablesGenerator, asList("a", 1));
		assertAtLeastOneGenerated(shrinkablesGenerator, asList("a", 2));
		assertAtLeastOneGenerated(shrinkablesGenerator, asList("a", 3));
		assertAtLeastOneGenerated(shrinkablesGenerator, asList("b", 1));
		assertAtLeastOneGenerated(shrinkablesGenerator, asList("b", 2));
		assertAtLeastOneGenerated(shrinkablesGenerator, asList("b", 3));
	}

	@Example
	void sameTypeVariableGetsSameArbitrary(@ForAll Random random) {

		ArbitraryResolver arbitraryResolver = new ArbitraryResolver() {
			@Override
			public Set<Arbitrary<?>> forParameter(MethodParameter parameter) {
				Set<Arbitrary<?>> arbitraries = new HashSet<>();
				arbitraries.add(Arbitraries.just("a"));
				arbitraries.add(Arbitraries.just("b"));
				return arbitraries;
			}
		};

		RandomizedShrinkablesGenerator shrinkablesGenerator = createGenerator(random, "twiceTypeVariableT", arbitraryResolver);

		assertAtLeastOneGenerated(shrinkablesGenerator, asList("a", "a"));
		assertAtLeastOneGenerated(shrinkablesGenerator, asList("b", "b"));
		assertNeverGenerated(shrinkablesGenerator, asList("a", "b"));
		assertNeverGenerated(shrinkablesGenerator, asList("b", "a"));
	}

	@Example
	void sameTypeVariableInParameterOfType(@ForAll Random random) {

		ArbitraryResolver arbitraryResolver = new ArbitraryResolver() {
			@Override
			public Set<Arbitrary<?>> forParameter(MethodParameter parameter) {
				Set<Arbitrary<?>> arbitraries = new HashSet<>();
				Arbitrary<String> a = Arbitraries.just("a");
				Arbitrary<String> b = Arbitraries.just("b");
				if (parameter.getType() instanceof TypeVariable) {
					arbitraries.add(a);
					arbitraries.add(b);
				} else {
					arbitraries.add(a.list().ofSize(1));
					arbitraries.add(b.list().ofSize(1));
				}
				return arbitraries;
			}
		};

		RandomizedShrinkablesGenerator shrinkablesGenerator = createGenerator(random, "typeVariableAlsoInList", arbitraryResolver);

		assertAtLeastOneGenerated(shrinkablesGenerator, asList("a", asList("a")));
		assertAtLeastOneGenerated(shrinkablesGenerator, asList("b", asList("b")));

		// TODO: This is really hard to implement and probably requires core changes in Arbitrary/RandomGenerator
		// assertNeverGenerated(shrinkablesGenerator, asList("a", asList("b")));
		// assertNeverGenerated(shrinkablesGenerator, asList("b", asList("a")));
	}

	private void assertAtLeastOneGenerated(ForAllParametersGenerator generator, List<Object> expected) {
		for (int i = 0; i < 500; i++) {
			ParameterSet<Shrinkable<Object>> shrinkables = generator.next();
			if (shrinkables.map(Shrinkable::value).getDirect().equals(expected))
				return;
		}
		fail("Failed to generate at least once");
	}

	private void assertNeverGenerated(ForAllParametersGenerator generator, List<Object> expected) {
		for (int i = 0; i < 500; i++) {
			ParameterSet<Shrinkable<Object>> shrinkables = generator.next();
			List<Object> values = shrinkables.map(Shrinkable::value).getDirect();
			if (values.equals(expected))
				fail(String.format("%s should never be generated", values));
		}
	}

	private RandomizedShrinkablesGenerator createGenerator(Random random, String methodName) {
		PropertyMethodArbitraryResolver arbitraryResolver = new PropertyMethodArbitraryResolver(
			new MyProperties(),
			DomainContext.global()
		);
		return createGenerator(random, methodName, arbitraryResolver);
	}

	private RandomizedShrinkablesGenerator createGenerator(Random random, String methodName, ArbitraryResolver arbitraryResolver) {
		PropertyMethodDescriptor methodDescriptor = createDescriptor(methodName);
		List<MethodParameter> parameters = TestHelper.getParameters(methodDescriptor);

		return RandomizedShrinkablesGenerator.forParameters(parameters, arbitraryResolver, random, 1000, EdgeCasesMode.NONE);
	}

	private PropertyMethodDescriptor createDescriptor(String methodName) {
		return TestHelper.createPropertyMethodDescriptor(MyProperties.class, methodName, "0", 1000, 5, ShrinkingMode.FULL);
	}

	private static class MyProperties {

		public void simpleParameters(@ForAll String aString, @ForAll int anInt) {}

		public <T> void twiceTypeVariableT(@ForAll T t1, @ForAll T t2) {}

		public <T> void typeVariableAlsoInList(@ForAll T t, @ForAll List<T> tList) {}
	}
}
