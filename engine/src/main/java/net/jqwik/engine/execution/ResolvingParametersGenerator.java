package net.jqwik.engine.execution;

import java.util.*;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;
import net.jqwik.api.lifecycle.ResolveParameterHook.*;
import net.jqwik.api.parameters.ParameterSet;
import net.jqwik.engine.properties.*;
import net.jqwik.engine.support.*;

public class ResolvingParametersGenerator implements ParametersGenerator {
	private final List<MethodParameter> propertyParameters;
	private final ForAllParametersGenerator forAllParametersGenerator;
	private final ParameterSupplierResolver parameterSupplierResolver;
	private int currentGenerationIndex = 0;

	public ResolvingParametersGenerator(
		List<MethodParameter> propertyParameters,
		ForAllParametersGenerator forAllParametersGenerator,
		ResolveParameterHook resolveParameterHook,
		PropertyLifecycleContext propertyLifecycleContext
	) {
		this.propertyParameters = propertyParameters;
		this.forAllParametersGenerator = forAllParametersGenerator;
		this.parameterSupplierResolver = new ParameterSupplierResolver(resolveParameterHook, propertyLifecycleContext);
	}

	@Override
	public boolean hasNext() {
		return forAllParametersGenerator.hasNext();
	}

	@Override
	public ParameterSet<Shrinkable<Object>> next(TryLifecycleContext context) {
		List<Shrinkable<Object>> next = new ArrayList<>();
		ParameterSet<Shrinkable<Object>> generated = forAllParametersGenerator.next();
		List<Shrinkable<Object>> forAllDirect = generated.getDirect();

		for (MethodParameter parameter : propertyParameters) {
			if (parameter.isAnnotated(ForAll.class)) {
				next.add(forAllDirect.get(0));
				forAllDirect.remove(0);
			} else {
				next.add(findResolvableParameter(parameter, context));
			}
		}

		currentGenerationIndex++;
		return new ParameterSet<>(next, generated.getDynamic());
	}

	@Override
	public int edgeCasesTotal() {
		return forAllParametersGenerator.edgeCasesTotal();
	}

	@Override
	public int edgeCasesTried() {
		return forAllParametersGenerator.edgeCasesTried();
	}

	@Override
	public GenerationInfo generationInfo(String randomSeed) {
		return new GenerationInfo(randomSeed, currentGenerationIndex);
	}

	private Shrinkable<Object> findResolvableParameter(MethodParameter parameter, TryLifecycleContext tryLifecycleContext) {
		ParameterSupplier parameterSupplier =
			parameterSupplierResolver.resolveParameter(parameter).orElseThrow(() -> {
				String info = "No matching resolver could be found";
				return new CannotResolveParameterException(parameter.getRawParameter(), info);
			});
		ParameterResolutionContext parameterContext = new DefaultParameterInjectionContext(parameter);
		return new ShrinkableResolvedParameter(parameterSupplier, parameterContext, tryLifecycleContext);
	}

	@Override
	public void reset() {
		currentGenerationIndex = 0;
		forAllParametersGenerator.reset();
	}
}
