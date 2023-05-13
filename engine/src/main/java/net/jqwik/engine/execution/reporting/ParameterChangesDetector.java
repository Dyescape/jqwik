package net.jqwik.engine.execution.reporting;

import java.lang.reflect.*;
import java.util.*;

import net.jqwik.api.*;
import net.jqwik.api.parameters.ParameterSet;

class ParameterChangesDetector {

	static boolean haveParametersChanged(ParameterSet<Object> before, ParameterSet<Object> after) {
		return atLeastOneParameterHasChanged(before, after);
	}

	private static boolean atLeastOneParameterHasChanged(ParameterSet<Object> before, ParameterSet<Object> after) {
		return atLeastOneHasChanged(before.getDirect(), after.getDirect())
				|| atLeastOneDynamicParameterHasChanged(before.getDynamic(), after.getDynamic());
	}

	private static boolean atLeastOneHasChanged(List<Object> before, List<Object> after) {
		if (before.size() != after.size()) {
			return true;
		}
		for (int i = 0; i < before.size(); i++) {
			Object beforeValue = before.get(i);
			Object afterValue = after.get(i);
			if (valuesDiffer(beforeValue, afterValue)) {
				return true;
			}
		}
		return false;
	}

	private static boolean atLeastOneDynamicParameterHasChanged(Map<String, Object> before, Map<String, Object> after) {
		Set<String> keySet = before.keySet();

		if (!keySet.equals(after.keySet())) {
			return true;
		}

		for (String key : keySet) {
			Object beforeValue = before.get(key);
			Object afterValue = after.get(key);
			if (valuesDiffer(beforeValue, afterValue)) {
				return true;
			}
		}

		return false;
	}

	private static boolean valuesDiffer(Object before, Object after) {
		if (Objects.isNull(before) != Objects.isNull(after)) {
			return true;
		}
		if (Objects.isNull(before)) {
			return false;
		}
		if (before.getClass() != after.getClass()) {
			return true;
		}
		if (before instanceof Tuple) {
			return tupleValuesDiffer((Tuple) before, (Tuple) after);
		}

		if (hasOwnEqualsImplementation(before.getClass())) {
			return !Objects.equals(before, after);
		} else {
			return false;
		}
	}

	private static boolean tupleValuesDiffer(Tuple before, Tuple after) {
		return atLeastOneHasChanged(before.items(), after.items());
	}

	private static boolean hasOwnEqualsImplementation(Class<?> aClass) {
		// TODO: There are probably other pathological cases of classes with equals implementation
		if (Proxy.isProxyClass(aClass)) {
			return false;
		}
		return !equalsMethod(aClass).equals(equalsMethod(Object.class));
	}

	private static Method equalsMethod(Class<?> aClass) {
		try {
			return aClass.getMethod("equals", Object.class);
		} catch (NoSuchMethodException e) {
			throw new JqwikException("All classes should have an equals() method");
		}
	}

}
