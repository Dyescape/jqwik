package net.jqwik.engine.properties;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.Shrinkable;
import net.jqwik.api.parameters.ParameterSet;
import net.jqwik.engine.execution.DynamicInfo;
import net.jqwik.engine.execution.GenerationInfo;

import java.util.*;


public interface ForAllParametersGenerator extends Iterator<ParameterSet<Shrinkable<Object>>> {
	ParameterSet<Shrinkable<Object>> peek(GenerationInfo info);

	int baseGenerationIndex();
	Map<String, Integer> dynamicProgress();
	default boolean edgeCase() {
		return false;
	}

	default int edgeCasesTotal() {
		return 0;
	}

	default int edgeCasesTried() {
		return 0;
	}

	default long requiredTries() {
		return 0;
	}

	Shrinkable<Object> peekDynamicParameter(String name, Arbitrary<Object> arbitrary, DynamicInfo info, boolean edgeCase);

	Shrinkable<Object> registerDynamicParameter(String name, Arbitrary<Object> arbitrary);
}
