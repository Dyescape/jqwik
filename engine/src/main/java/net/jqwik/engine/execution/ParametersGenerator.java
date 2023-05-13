package net.jqwik.engine.execution;

import net.jqwik.api.Shrinkable;
import net.jqwik.api.lifecycle.*;
import net.jqwik.api.parameters.ParameterSet;

public interface ParametersGenerator {

    boolean hasNext();

    ParameterSet<Shrinkable<Object>> next(TryLifecycleContext context);

    int edgeCasesTotal();

    int edgeCasesTried();

    GenerationInfo generationInfo(String randomSeed);

    void reset();
}
