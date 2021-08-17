package net.jqwik.engine.hooks;

import java.lang.reflect.*;
import java.util.*;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;

public class ResolveFootnotesHook implements ResolveParameterHook, AroundTryHook {

	@Override
	public PropagationMode propagateTo() {
		return PropagationMode.ALL_DESCENDANTS;
	}

	@Override
	public Optional<ParameterSupplier> resolve(
		ParameterResolutionContext parameterContext,
		LifecycleContext lifecycleContext
	) {
		if (parameterContext.typeUsage().isOfType(Footnotes.class)) {
			ParameterSupplier footnotesSupplier = optionalTry -> {
				Store<List<String>> footnotesStore = optionalTry
					.map(ignore -> getFootnotesStore())
					.orElseThrow(() -> {
						String message = String.format(
							"Illegal argument [%s] in method [%s].%n" +
								"Objects of type %s can only be injected directly " +
								"in property methods or in @BeforeTry and @AfterTry " +
								"lifecycle methods.",
							parameterContext.parameter(),
							parameterContext.optionalMethod().map(Method::toString).orElse("unknown"),
							Footnotes.class
						);
						return new IllegalArgumentException(message);
					});

				return (Footnotes) footnote -> {
					footnotesStore.get().add(footnote);
				};
			};
			return Optional.of(footnotesSupplier);
		}
		return Optional.empty();
	}

	private Store<List<String>> getFootnotesStore() {
		return Store.getOrCreate(
			Tuple.of(ResolveFootnotesHook.class, "footnotes"),
			Lifespan.TRY, ArrayList::new
		);
	}

	@Override
	public TryExecutionResult aroundTry(TryLifecycleContext context, TryExecutor aTry, List<Object> parameters) {
		TryExecutionResult executionResult = aTry.execute(parameters);
		if (executionResult.isFalsified()) {
			List<String> footnotes = getFootnotes();
			return executionResult.withFootnotes(footnotes);
		}
		return executionResult;
	}

	@Override
	public int aroundTryProximity() {
		return Hooks.AroundTry.TRY_RESOLVE_FOOTNOTES_PROXIMITY;
	}

	private List<String> getFootnotes() {
		return getFootnotesStore().get();
	}
}
