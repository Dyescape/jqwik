package net.jqwik.engine.dynamic;

import net.jqwik.api.dynamic.MissingDynamicContextException;

import java.util.function.Supplier;

public class CurrentDynamicContext {
    private static final ThreadLocal<DynamicContext> currentContext = new ThreadLocal<>();

    public static DynamicContext get() {
        DynamicContext context = currentContext.get();

        if (context == null) {
            throw new MissingDynamicContextException();
        }

        return context;
    }

    public static <T> T runWithContext(DynamicContext context, Supplier<T> runnable) {
        currentContext.set(context);

        try {
            return runnable.get();
        } finally {
            if (currentContext.get() == context) {
                currentContext.remove();
            }
        }
    }
}
