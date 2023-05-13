package net.jqwik.engine.properties;

import java.util.*;

import net.jqwik.api.*;
import net.jqwik.api.parameters.ParameterSet;
import net.jqwik.engine.support.*;

import static java.lang.Math.*;

public class EdgeCasesGenerator implements Iterator<ParameterSet<Shrinkable<Object>>> {

	// Caveat: Always make sure that the number is greater than 1.
	// Otherwise only edge cases will be generated
	// Currently the value is always between 5 and 20
	public static int calculateBaseToEdgeCaseRatio(int genSize, int countEdgeCases) {
		return min(
			max(genSize / countEdgeCases, 5),
			20
		);
	}

	private final ParameterSet<EdgeCases<Object>> edgeCases;
	private final Iterator<ParameterSet<Shrinkable<Object>>> iterator;

	EdgeCasesGenerator(ParameterSet<EdgeCases<Object>> edgeCases) {
		this.edgeCases = edgeCases;
		this.iterator = createIterator();
	}

	private Iterator<ParameterSet<Shrinkable<Object>>> createIterator() {
		if (this.edgeCases.isEmpty()) {
			return Collections.emptyIterator();
		}
		ParameterSet<Iterable<Shrinkable<Object>>> iterables = edgeCases.map(edge -> edge);
		return Combinatorics.combineParameters(iterables);
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public ParameterSet<Shrinkable<Object>> next() {
		return iterator.next();
	}
}
