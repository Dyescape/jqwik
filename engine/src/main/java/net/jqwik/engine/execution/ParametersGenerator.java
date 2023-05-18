package net.jqwik.engine.execution;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.Shrinkable;
import net.jqwik.api.lifecycle.TryLifecycleContext;
import net.jqwik.api.parameters.ParameterSet;

public interface ParametersGenerator {

    boolean hasNext();

    ParameterSet<Shrinkable<Object>> next(TryLifecycleContext context);

    ParameterSet<Shrinkable<Object>> peek(GenerationInfo info, TryLifecycleContext context);

    int edgeCasesTotal();

    int edgeCasesTried();

    default long requiredTries() {
        return 0;
    }

    GenerationInfo generationInfo(String randomSeed);

    <V> V registerDynamicParameter(String name, Arbitrary<V> arbitrary);
}
