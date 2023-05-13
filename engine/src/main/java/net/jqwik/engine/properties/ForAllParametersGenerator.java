package net.jqwik.engine.properties;

import net.jqwik.api.Shrinkable;
import net.jqwik.api.parameters.ParameterSet;

import java.util.*;


public interface ForAllParametersGenerator extends Iterator<ParameterSet<Shrinkable<Object>>> {

	default int edgeCasesTotal() {
		return 0;
	}

	default int edgeCasesTried() {
		return 0;
	}

    void reset();
}
