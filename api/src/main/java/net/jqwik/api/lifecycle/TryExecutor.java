package net.jqwik.api.lifecycle;

import net.jqwik.api.parameters.ParameterSet;
import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.MAINTAINED;

/**
 * Experimental feature. Not ready for public usage yet.
 */
@API(status = MAINTAINED, since = "1.4.0")
public interface TryExecutor {

    TryExecutionResult execute(ParameterSet<Object> parameters);
}
