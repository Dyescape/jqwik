package net.jqwik.engine.properties.shrinking;

import net.jqwik.api.Falsifier;
import net.jqwik.api.Shrinkable;
import net.jqwik.api.lifecycle.FalsifiedSample;
import net.jqwik.api.lifecycle.TryExecutionResult;
import net.jqwik.api.parameters.ParameterReference;
import net.jqwik.api.parameters.ParameterSet;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

class OneAfterTheOtherParameterShrinker extends AbstractSampleShrinker {

    public OneAfterTheOtherParameterShrinker(Map<ParameterSet<Object>, TryExecutionResult> falsificationCache) {
        super(falsificationCache);
    }

    @Override
    public FalsifiedSample shrink(
            Falsifier<ParameterSet<Object>> falsifier,
            FalsifiedSample sample,
            Consumer<FalsifiedSample> sampleShrunkConsumer,
            Consumer<FalsifiedSample> shrinkAttemptConsumer
    ) {
        FalsifiedSample current = sample;
        for (ParameterReference reference : sample.shrinkables().references()) {
            current = shrinkSingleParameter(falsifier, current, sampleShrunkConsumer, shrinkAttemptConsumer, reference);
        }
        return current;
    }

    private FalsifiedSample shrinkSingleParameter(
            Falsifier<ParameterSet<Object>> falsifier,
            FalsifiedSample sample,
            Consumer<FalsifiedSample> sampleShrunkConsumer,
            Consumer<FalsifiedSample> shrinkAttemptConsumer,
            ParameterReference parameterIndex
    ) {
        Function<ParameterSet<Shrinkable<Object>>, Stream<ParameterSet<Shrinkable<Object>>>> shrinker =
                shrinkables -> {
                    Shrinkable<Object> shrinkable = shrinkables.get(parameterIndex);
                    return shrinkable.shrink().map(s -> sample.shrinkables().with(parameterIndex, s));
                };

        return shrink(
                falsifier,
                sample,
                sampleShrunkConsumer,
                shrinkAttemptConsumer,
                shrinker
        );
    }
}
