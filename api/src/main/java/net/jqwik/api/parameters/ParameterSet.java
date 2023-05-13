package net.jqwik.api.parameters;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ParameterSet<V> {
    private final List<V> direct;
    private final SortedMap<String, V> dynamic;

    public ParameterSet(List<V> direct, Map<String, V> dynamic) {
        this.direct = direct;
        this.dynamic = new TreeMap<>(String::compareTo);
        this.dynamic.putAll(dynamic);
    }

    public List<V> getDirect() {
        return direct;
    }

    public SortedMap<String, V> getDynamic() {
        return dynamic;
    }

    public int directSize() {
        return direct.size();
    }

    public V get(int index) {
        return direct.get(index);
    }

    public void set(int index, V value) {
        direct.set(index, value);
    }

    public V getDynamic(String key) {
        return dynamic.get(key);
    }

    public void setDynamic(String key, V value) {
        dynamic.put(key, value);
    }

    public V get(ParameterReference reference) {
        return reference.from(this);
    }

    public void set(ParameterReference reference, V value) {
        reference.update(this, value);
    }

    public ParameterSet<V> with(ParameterReference reference, V value) {
        ParameterSet<V> copy = copy();
        copy.set(reference, value);
        return copy;
    }

    public ParameterSet<V> copy() {
        return new ParameterSet<>(new ArrayList<>(direct), new HashMap<>(dynamic));
    }

    public <M> ParameterSet<M> map(Function<V, M> mapper) {
        return new ParameterSet<>(
                direct.stream()
                        .map(mapper)
                        .collect(Collectors.toList()),
                dynamic.entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> mapper.apply(entry.getValue())))
        );
    }

    public boolean isEmpty() {
        return direct.isEmpty() && dynamic.isEmpty();
    }

    public List<V> all() {
        List<V> all = new ArrayList<>();
        all.addAll(direct);
        all.addAll(dynamic.values());
        return all;
    }

    public List<ParameterReference> references() {
        List<ParameterReference> indices = new ArrayList<>();

        for (int i = 0; i < direct.size(); i++) {
            indices.add(new ParameterReference.Direct(i));
        }

        for (String key : dynamic.keySet()) {
            indices.add(new ParameterReference.Dynamic(key));
        }

        return indices;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParameterSet)) return false;
        ParameterSet<?> that = (ParameterSet<?>) o;
        return Objects.equals(getDirect(), that.getDirect()) && Objects.equals(getDynamic(), that.getDynamic());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDirect(), getDynamic());
    }

    public static <T> ParameterSet<T> empty() {
        return new ParameterSet<>(new ArrayList<>(), new HashMap<>());
    }

    public static <T> ParameterSet<T> direct(Collection<T> direct) {
        return new ParameterSet<>(new ArrayList<>(direct), new HashMap<>());
    }

    public static <T> ParameterSet<T> dynamic(Map<String, T> dynamic) {
        return new ParameterSet<>(new ArrayList<>(), new HashMap<>(dynamic));
    }
}
