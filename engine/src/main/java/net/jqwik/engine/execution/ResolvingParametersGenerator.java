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
	private List<String> dynamicIntroductions = new ArrayList<>();
	private GenerationInfo activePeek;

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
		this.activePeek = null;
		currentGenerationIndex++;

		ParameterSet<Shrinkable<Object>> generated = forAllParametersGenerator.next();

		return injectParameters(generated, context);
	}

	@Override
	public ParameterSet<Shrinkable<Object>> peek(GenerationInfo info, TryLifecycleContext context) {
		this.activePeek = info;

		for (int index = 0; index < info.generationIndex(); index++) {
			for (MethodParameter parameter : propertyParameters) {
				if (!parameter.isAnnotated(ForAll.class)) {
					findResolvableParameter(parameter, context);
				}
			}
		}

		ParameterSet<Shrinkable<Object>> generated =  forAllParametersGenerator.peek(info);

		return injectParameters(generated, context);
	}

	private ParameterSet<Shrinkable<Object>> injectParameters(
			ParameterSet<Shrinkable<Object>> generated,
			TryLifecycleContext context
	) {
		List<Shrinkable<Object>> next = new ArrayList<>();
		List<Shrinkable<Object>> forAllDirect = generated.getDirect();

		for (MethodParameter parameter : propertyParameters) {
			if (parameter.isAnnotated(ForAll.class)) {
				next.add(forAllDirect.get(0));
				forAllDirect.remove(0);
			} else {
				next.add(findResolvableParameter(parameter, context));
			}
		}

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
	public long requiredTries() {
		return forAllParametersGenerator.requiredTries();
	}

	@Override
	public GenerationInfo generationInfo(String rootSeed) {
		Map<String, Integer> progress = forAllParametersGenerator.dynamicProgress();
		Map<String, DynamicInfo> dynamics = new HashMap<>();

		for (int i = 0; i < dynamicIntroductions.size(); i++) {
			String name = dynamicIntroductions.get(i);

			dynamics.put(name, new DynamicInfo(i, progress.get(name)));
		}

		return new GenerationInfo(
				rootSeed,
				currentGenerationIndex,
				forAllParametersGenerator.baseGenerationIndex(),
				forAllParametersGenerator.edgeCase(),
				dynamics
		);
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
	@SuppressWarnings("unchecked")
	public <V> V registerDynamicParameter(String name, Arbitrary<V> arbitrary) {
		Shrinkable<Object> shrinkable;

		if (activePeek != null) {
			DynamicInfo info = activePeek.dynamics().get(name);
			if (info == null) {
				throw new JqwikException("Dynamic not known: " + name);
			}

			shrinkable = forAllParametersGenerator.peekDynamicParameter(
					name,
					arbitrary.map(a -> a),
					info,
					activePeek.edgeCase()
			);
		} else {
			shrinkable = forAllParametersGenerator.registerDynamicParameter(name, arbitrary.map(a -> a));

			dynamicIntroductions.add(name);
		}

        return shrinkable != null ? (V) shrinkable.value() : null;
	}
}
