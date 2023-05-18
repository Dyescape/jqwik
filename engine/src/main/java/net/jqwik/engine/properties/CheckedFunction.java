package net.jqwik.engine.properties;

import java.util.function.*;

import net.jqwik.api.JqwikException;
import net.jqwik.api.parameters.ParameterSet;
import org.opentest4j.*;

import net.jqwik.api.lifecycle.*;
import net.jqwik.engine.execution.lifecycle.*;

@FunctionalInterface
public interface CheckedFunction extends Predicate<ParameterSet<Object>>, TryExecutor, TryLifecycleExecutor {

	@Override
	default TryExecutionResult execute(ParameterSet<Object> parameters) {
		try {
			boolean result = this.test(parameters);
			return result ? TryExecutionResult.satisfied() : TryExecutionResult.falsified(null);
		} catch (JqwikException e){
			throw e;
		} catch (TestAbortedException tea) {
			return TryExecutionResult.invalid(tea);
		} catch (AssertionError | Exception e) {
			return TryExecutionResult.falsified(e);
		}
	}

	/**
	 * Only needed to simplify some tests
	 */
	@Override
	default TryExecutionResult execute(TryLifecycleContext tryLifecycleContext, ParameterSet<Object> parameters) {
		return execute(parameters);
	}
}
