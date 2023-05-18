package net.jqwik.engine.dynamic;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.parameters.ParameterSet;
import net.jqwik.engine.execution.ParametersGenerator;

import java.util.function.Supplier;

public class PropertyCheckDynamicContext implements DynamicContext {
    private final ParametersGenerator generator;
    private ParameterSet<Object> currentParameters;

    public PropertyCheckDynamicContext(ParametersGenerator generator) {
        this.generator = generator;
    }

    public void startTry(ParameterSet<Object> currentParameters) {
        this.currentParameters = currentParameters;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V parameter(String name, Supplier<Arbitrary<V>> provider) {
        if (currentParameters.hasDynamic(name)) {
            return (V) currentParameters.getDynamic(name);
        }

        Arbitrary<V> arbitrary = provider.get();
        V value = generator.registerDynamicParameter(name, arbitrary);
        currentParameters.setDynamic(name, value);

        return value;
    }
}
