package net.jqwik.engine.properties;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.JqwikException;
import net.jqwik.api.Shrinkable;
import net.jqwik.api.Tuple;
import net.jqwik.api.parameters.ParameterSet;
import net.jqwik.api.providers.TypeUsage;
import net.jqwik.engine.execution.DynamicInfo;
import net.jqwik.engine.execution.GenerationInfo;
import net.jqwik.engine.support.JqwikStringSupport;
import net.jqwik.engine.support.MethodParameter;
import net.jqwik.engine.support.types.TypeUsageImpl;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DataBasedShrinkablesGenerator implements ForAllParametersGenerator {

	private final List<MethodParameter> forAllParameters;
	private final Iterable<? extends Tuple> data;
	private final Iterator<? extends Tuple> iterator;
	private int baseIndex = -1;

	public DataBasedShrinkablesGenerator(List<MethodParameter> forAllParameters, Iterable<? extends Tuple> data) {
		this.forAllParameters = forAllParameters;
		this.data = data;
		this.iterator = data.iterator();
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public ParameterSet<Shrinkable<Object>> next() {
		baseIndex++;

		return transform(iterator.next());
	}

	@Override
	public ParameterSet<Shrinkable<Object>> peek(GenerationInfo info) {
		if (!info.dynamics().isEmpty()) {
			throw new IllegalStateException();
		}

		Iterator<? extends Tuple> iterator = data.iterator();

		for (int i = 0; i < info.baseGenerationIndex(); i++) {
			iterator.next();
		}

		return transform(iterator.next());
	}

	@Override
	public int baseGenerationIndex() {
		return baseIndex;
	}

	@Override
	public Map<String, Integer> dynamicProgress() {
		return Collections.emptyMap();
	}

	private ParameterSet<Shrinkable<Object>> transform(Tuple tuple) {
		checkCompatibility(tuple);

		List<Shrinkable<Object>> direct = tuple.items()
				.stream()
				.map(Shrinkable::unshrinkable).collect(Collectors.toList());

		return ParameterSet.direct(direct);
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

	@Override
	public Shrinkable<Object> peekDynamicParameter(String name, Arbitrary<Object> arbitrary, DynamicInfo info, boolean edgeCase) {
		throw new JqwikException("Dynamic parameters are not compatible with @FromData");
	}

    @Override
    public Shrinkable<Object> registerDynamicParameter(String name, Arbitrary<Object> arbitrary) {
        throw new JqwikException("Dynamic parameters are not compatible with @FromData");
    }
}
