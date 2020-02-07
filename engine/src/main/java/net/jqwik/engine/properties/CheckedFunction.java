package net.jqwik.engine.properties;

import java.util.*;
import java.util.function.*;

import org.opentest4j.*;

import net.jqwik.api.lifecycle.*;
import net.jqwik.engine.support.*;

@FunctionalInterface
public interface CheckedFunction extends Predicate<List<Object>>, TryExecutor {

	@Override
	default TryExecutionResult execute(List<Object> parameters) {
		try {
			boolean result = this.test(parameters);
			return result ? TryExecutionResult.satisfied() : TryExecutionResult.falsified(null);
		} catch (TestAbortedException tea) {
			return TryExecutionResult.invalid();
		} catch (AssertionError | Exception e) {
			return TryExecutionResult.falsified(e);
		} catch (Throwable throwable) {
			JqwikExceptionSupport.rethrowIfBlacklisted(throwable);
			return JqwikExceptionSupport.throwAsUncheckedException(throwable);
		}
	}

}
