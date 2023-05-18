package examples.packageWithDynamicParamters;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.*;

import java.util.List;
import java.util.Set;

public class DynamicParametersTests {

	@Property
	boolean allIntegersAndNulls(@ForAll @WithNull Integer anInt) {
		return anInt != null;
	}
}
