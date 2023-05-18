package net.jqwik.api.lifecycle;

import java.util.*;

import net.jqwik.api.Shrinkable;
import net.jqwik.api.parameters.ParameterSet;
import org.apiguardian.api.*;

import static org.apiguardian.api.API.Status.*;

/**
 * A falsified sample is the collection of data that could be falsified during
 * a property run.
 *
 * @see ShrunkFalsifiedSample
 * @see PropertyExecutionResult#shrunkSample()
 */
@API(status = MAINTAINED, since = "1.3.5")
public interface FalsifiedSample {

	/**
	 * The actual parameters used when running a property method.
	 *
	 * <p>
	 * If parameters are muted during the run,
	 * e.g. elements have been added to a mutable collection,
	 * this method returns the muted objects.
	 * </p>
	 *
	 * @return list of objects of same size as list returned by {@linkplain #shrinkables()}.
	 */
	ParameterSet<Object> parameters();

	/**
	 * The list of shrinkables that were used to generate the parameters.
	 * The position of shrinkables corresponds to the actual parameter object in {@linkplain #parameters()}.
	 *
	 * <p>
	 * You can create a fresh, unchanged list of parameter objects through
	 * {@code sample.shrinkables().stream(Shrinkable::value).collect(Collectors.toList())}.
	 * </p>
	 *
	 * @return list of shrinkables of same size as list returned by {@linkplain #parameters()}.
	 */
	ParameterSet<Shrinkable<Object>> shrinkables();

	/**
	 * The error which resulted in falsifying a property.
	 * If the property was falsified by return false this method returns {@code Optional.empty()}.
	 *
	 * @return an optional error
	 */
	Optional<Throwable> falsifyingError();

	/**
	 * List of footnotes to be added to failure report.
	 *
	 * @return list of strings
	 */
	@API(status = MAINTAINED, since = "1.5.5")
	List<String> footnotes();

}
