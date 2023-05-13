package net.jqwik.engine.execution.lifecycle;

import net.jqwik.api.lifecycle.*;
import net.jqwik.api.parameters.ParameterSet;

public interface TryLifecycleExecutor {

	TryExecutionResult execute(TryLifecycleContext tryLifecycleContext, ParameterSet<Object> parameters);
}
