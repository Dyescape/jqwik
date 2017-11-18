package net.jqwik.execution;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.platform.commons.support.*;

import net.jqwik.api.ForAll;
import net.jqwik.descriptor.*;
import net.jqwik.properties.CheckedFunction;

public class CheckedPropertyFactory {

	private static List<Class<?>> BOOLEAN_RETURN_TYPES = Arrays.asList(boolean.class, Boolean.class);

	public CheckedProperty fromDescriptor(PropertyMethodDescriptor propertyMethodDescriptor, Object testInstance) {
		String propertyName = propertyMethodDescriptor.getLabel();

		Method propertyMethod = propertyMethodDescriptor.getTargetMethod();
		PropertyConfiguration configuration = propertyMethodDescriptor.getConfiguration();

		CheckedFunction forAllPredicate = createForAllPredicate(propertyMethodDescriptor, testInstance);
		List<Parameter> forAllParameters = extractForAllParameters(propertyMethod);
		PropertyMethodArbitraryResolver arbitraryProvider = new PropertyMethodArbitraryResolver(propertyMethodDescriptor, testInstance);
		return new CheckedProperty(propertyName, forAllPredicate, forAllParameters, arbitraryProvider, configuration);
	}

	private CheckedFunction createForAllPredicate(PropertyMethodDescriptor propertyMethodDescriptor, Object testInstance) {
		// Todo: Bind all non @ForAll params first
		Class<?> returnType = propertyMethodDescriptor.getTargetMethod().getReturnType();
		Function<List, Object> function = params -> ReflectionSupport.invokeMethod(propertyMethodDescriptor.getTargetMethod(), testInstance,
				params.toArray());
		if (BOOLEAN_RETURN_TYPES.contains(returnType))
			return params -> (boolean) function.apply(params);
		else
			return params -> {
				function.apply(params);
				return true;
			};
	}

	private List<Parameter> extractForAllParameters(Method targetMethod) {
		return Arrays //
				.stream(targetMethod.getParameters()) //
				.filter(this::isForAllPresent) //
				.collect(Collectors.toList());
	}

	private boolean isForAllPresent(Parameter parameter) {
		return AnnotationSupport.isAnnotated(parameter, ForAll.class);
	}

}
