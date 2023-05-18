package net.jqwik.api.parameters;

public interface ParameterReference {
    <V> V from(ParameterSet<V> parameters);

    <V> void update(ParameterSet<V> parameters, V value);

    String toString();

    class Direct implements ParameterReference {
        private final int index;

        public Direct(int index) {
            this.index = index;
        }

        @Override
        public <V> V from(ParameterSet<V> parameters) {
            return parameters.get(index);
        }

        @Override
        public <V> void update(ParameterSet<V> parameters, V value) {
            parameters.set(index, value);
        }

        @Override
        public String toString() {
            return Integer.toString(index);
        }
    }

    class Dynamic implements ParameterReference {
        private final String key;

        public Dynamic(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        @Override
        public <V> V from(ParameterSet<V> parameters) {
            return parameters.getDynamic(key);
        }

        @Override
        public <V> void update(ParameterSet<V> parameters, V value) {
            parameters.setDynamic(key, value);
        }

        @Override
        public String toString() {
            return key;
        }
    }
}
