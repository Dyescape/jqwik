package net.jqwik.engine.properties;

import java.util.*;
import java.util.stream.*;

import net.jqwik.api.*;
import net.jqwik.api.parameters.ParameterSet;
import net.jqwik.api.providers.*;
import net.jqwik.engine.support.*;
import net.jqwik.engine.support.types.*;

public class DataBasedShrinkablesGenerator implements ForAllParametersGenerator {

	private final List<MethodParameter> forAllParameters;
	private final Iterable<? extends Tuple> data;
	private Iterator<? extends Tuple> iterator;

	public DataBasedShrinkablesGenerator(List<MethodParameter> forAllParameters, Iterable<? extends Tuple> data) {
		this.forAllParameters = forAllParameters;
		this.data = data;
		this.reset();
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public ParameterSet<Shrinkable<Object>> next() {
		Tuple tuple = iterator.next();
		checkCompatibility(tuple);

		List<Shrinkable<Object>> direct = tuple.items()
				.stream()
				.map(Shrinkable::unshrinkable).collect(Collectors.toList());

		return ParameterSet.direct(direct);
	}

	@Override
	public void reset() {
		this.iterator = this.data.iterator();
	}

	private void checkCompatibility(Tuple tuple) {
		if (tuple.size() != forAllParameters.size()) {
			throw new IncompatibleDataException(createIncompatibilityMessage(tuple));
		}
		for (int i = 0; i < tuple.items().size(); i++) {
			Object value = tuple.items().get(i);
			TypeUsage parameterType = TypeUsageImpl.forParameter(forAllParameters.get(i));
			if (value == null) {
				if (parameterType.getRawType().isPrimitive()) {
					throw new IncompatibleDataException(createIncompatibilityMessage(tuple));
				}
			} else {
				TypeUsage valueType = TypeUsage.of(value.getClass());
				if (!valueType.canBeAssignedTo(parameterType)) {
					throw new IncompatibleDataException(createIncompatibilityMessage(tuple));
				}
			}
		}
	}

	private String createIncompatibilityMessage(Tuple tuple) {
		List<TypeUsage> parameterTypes =
			this.forAllParameters
				.stream()
				.map(TypeUsageImpl::forParameter)
				.collect(Collectors.toList());

		return String.format(
			"Data tuple %s is not compatible with parameters %s",
			tuple,
			JqwikStringSupport.displayString(parameterTypes)
		);
	}
}
