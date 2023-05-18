package net.jqwik.api.dynamic;

public class MissingDynamicContextException extends RuntimeException {
    public MissingDynamicContextException() {
        super("Missing dynamic context");
    }
}
