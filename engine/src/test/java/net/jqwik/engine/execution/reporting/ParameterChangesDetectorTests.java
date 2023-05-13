package net.jqwik.engine.execution.reporting;

import java.util.*;

import net.jqwik.api.*;
import net.jqwik.api.parameters.ParameterSet;

import static org.assertj.core.api.Assertions.*;

import static net.jqwik.engine.execution.reporting.ParameterChangesDetector.*;

class ParameterChangesDetectorTests {

	@Example
	void emptyParametersAreUnchanged() {
		assertThat(haveParametersChanged(ParameterSet.empty(), ParameterSet.empty())).isFalse();
	}

	@Example
	void identicalDirectParametersAreUnchanged() {
        ParameterSet<Object> before = create(Arrays.asList(new Object(), "a String", 42, null));
		assertThat(haveParametersChanged(before, before)).isFalse();
	}

	@Example
	void identicalDynamicParametersAreUnchanged() {
        ParameterSet<Object> before = create(dynamicParameters(new Object(), "a String", 42, null));
		assertThat(haveParametersChanged(before, before)).isFalse();
	}

	@Example
	void identicalCombinedParametersAreUnchanged() {
        ParameterSet<Object> before = create(
            Arrays.asList(new Object(), "a String", 42, null),
            dynamicParameters(new Object(), "a String", 42, null)
        );

		assertThat(haveParametersChanged(before, before)).isFalse();
	}

	@Example
	void copiedDirectParametersAreUnchanged() {
        ParameterSet<Object> before = create(Arrays.asList(new Object(), "a String", 42, null));
        ParameterSet<Object> after = before.copy();
		assertThat(haveParametersChanged(before, after)).isFalse();
	}

	@Example
	void copiedDynamicParametersAreUnchanged() {
        ParameterSet<Object> before = create(dynamicParameters(new Object(), "a String", 42, null));
        ParameterSet<Object> after = before.copy();
		assertThat(haveParametersChanged(before, after)).isFalse();
	}

	@Example
	void copiedCombinedParametersAreUnchanged() {
        ParameterSet<Object> before = create(
                Arrays.asList(new Object(), "a String", 42, null),
                dynamicParameters(new Object(), "a String", 42, null)
        );

        ParameterSet<Object> after = before.copy();
		assertThat(haveParametersChanged(before, after)).isFalse();
	}

	@Example
	void changedDirectObjectParameterDoesNotCountAsChange() {
        ParameterSet<Object> before = create(Arrays.asList(new Object(), "a String", 42, null));
        ParameterSet<Object> after = create(Arrays.asList(new Object(), "a String", 42, null));
		assertThat(haveParametersChanged(before, after)).isFalse();
	}

	@Example
	void changedDynamicObjectParameterDoesNotCountAsChange() {
        ParameterSet<Object> before = create(dynamicParameters(new Object(), "a String", 42, null));
        ParameterSet<Object> after = create(dynamicParameters(new Object(), "a String", 42, null));
		assertThat(haveParametersChanged(before, after)).isFalse();
	}

	@Example
	void changedObjectDoesNotCountAsChangeInTuple() {
        ParameterSet<Object> before = create(Arrays.asList(Tuple.of(new Object(), "a String")));
        ParameterSet<Object> after = create(Arrays.asList(Tuple.of(new Object(), "a String")));
		assertThat(haveParametersChanged(before, after)).isFalse();
	}

	@Example
	void changedObjectDoesNotCountAsChangeInDynamicTuple() {
        ParameterSet<Object> before = create(dynamicParameters(Tuple.of(new Object(), "a String")));
        ParameterSet<Object> after = create(dynamicParameters(Tuple.of(new Object(), "a String")));
		assertThat(haveParametersChanged(before, after)).isFalse();
	}

	@Example
	void oneParameterLess() {
		ParameterSet<Object> before = create(Arrays.asList(new Object(), "a String", 42, null));
		ParameterSet<Object> after = create(Arrays.asList(new Object(), "a String", 41));
		assertThat(haveParametersChanged(before, after)).isTrue();
		assertThat(haveParametersChanged(after, before)).isTrue();
	}

	@Example
	void oneDynamicParameterLess() {
		ParameterSet<Object> before = create(dynamicParameters(new Object(), "a String", 42, null));
		ParameterSet<Object> after = create(dynamicParameters(new Object(), "a String", 41));
		assertThat(haveParametersChanged(before, after)).isTrue();
		assertThat(haveParametersChanged(after, before)).isTrue();
	}

	@Example
	void oneParameterChanged() {
        ParameterSet<Object> before = create(Arrays.asList(new Object(), "a String", 42));
        ParameterSet<Object> after = create(Arrays.asList(new Object(), "a String", 41));
		assertThat(haveParametersChanged(before, after)).isTrue();
	}

	@Example
	void oneDynamicParameterChanged() {
        ParameterSet<Object> before = create(dynamicParameters(new Object(), "a String", 42));
        ParameterSet<Object> after = create(dynamicParameters(new Object(), "a String", 41));
		assertThat(haveParametersChanged(before, after)).isTrue();
	}

	@Example
	void oneParameterChangedInTuple() {
        ParameterSet<Object> before = create(Arrays.asList(Tuple.of(new Object(), "a String", 42)));
        ParameterSet<Object> after = create(Arrays.asList(Tuple.of(new Object(), "a String", 41)));
		assertThat(haveParametersChanged(before, after)).isTrue();
	}

	@Example
	void oneParameterChangedInDynamicTuple() {
        ParameterSet<Object> before = create(dynamicParameters(Tuple.of(new Object(), "a String", 42)));
        ParameterSet<Object> after = create(dynamicParameters(Tuple.of(new Object(), "a String", 41)));
		assertThat(haveParametersChanged(before, after)).isTrue();
	}

	@Example
	void nestedFieldHasChanged() {
		Node node1 = new Node(1);
		node1.next = new Node(2);

		Node node11 = new Node(1);
		node11.next = new Node(2);

		List<Object> before = Arrays.asList(node1);
		List<Object> after = Arrays.asList(node11);
		assertThat(haveParametersChanged(create(before), create(after))).isFalse();

		node11.next.value = 3;
		assertThat(haveParametersChanged(create(before), create(after))).isTrue();
	}

	@Example
	void nullParameterChanged() {
		List<Object> before = Arrays.asList(new Object(), "a String", null);
		List<Object> after = Arrays.asList(new Object(), "a String", 41);
		assertThat(haveParametersChanged(create(before), create(after))).isTrue();
		assertThat(haveParametersChanged(create(after), create(before))).isTrue();
	}

	@Example
	void parametersWithDifferentClassesHaveChanged() {
		List<Object> before = Arrays.asList(new Object());
		List<Object> after = Arrays.asList(new Object() {});
		assertThat(haveParametersChanged(create(before), create(after))).isTrue();
		assertThat(haveParametersChanged(create(after), create(before))).isTrue();
	}

    private static ParameterSet<Object> create(List<Object> direct) {
        return ParameterSet.direct(direct);
    }

    private static ParameterSet<Object> create(Map<String, Object> dynamic) {
        return ParameterSet.dynamic(dynamic);
    }

    private static ParameterSet<Object> create(List<Object> direct, Map<String, Object> dynamic) {
        return new ParameterSet<>(direct, dynamic);
    }

    private static final Iterable<String> DYNAMIC_KEYS = Arrays.asList("first", "second", "third", "fourth", "fifth");
	private static Map<String, Object> dynamicParameters(Object... values) {
        Iterator<String> keys = DYNAMIC_KEYS.iterator();
        Map<String, Object> result = new HashMap<>();

        for (Object value : values) {
            result.put(keys.next(), value);
        }

        return result;
	}

	private static class Node {
		int value;
		Node next = null;

		private Node(int value) {
			this.value = value;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Node node = (Node) o;
			return value == node.value && Objects.equals(next, node.next);
		}

		@Override
		public int hashCode() {
			return Objects.hash(value, next);
		}
	}
}
