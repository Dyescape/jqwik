package net.jqwik.engine.support.combinatorics;

import net.jqwik.api.parameters.ParameterReference;
import net.jqwik.api.parameters.ParameterSet;

import java.util.*;

public class CombinedParameterIterator<T> implements Iterator<ParameterSet<T>> {
	private final List<ParameterReference> references;
	private final ParameterSet<Iterable<T>> iterables;
	private final ParameterSet<Iterator<T>> iterators;
	private final ParameterSet<T> elements;
	private boolean isEmpty;
	private boolean publishFirstEmpty;
	private int position = -1;
	private int directIteration = -1;
	private Map<String, Integer> dynamicIterations;

	public CombinedParameterIterator(ParameterSet<Iterable<T>> iterables) {
		this.iterables = iterables;
		references = iterables.references();
		elements = iterables.map(i -> null);
		iterators = iterables.map(Iterable::iterator);
		publishFirstEmpty = iterables.isEmpty();
		isEmpty = iterables.isEmpty() || !iterators.all().stream().allMatch(Iterator::hasNext);

		dynamicIterations = new HashMap<>();
		for (String key : iterables.getDynamic().keySet()) {
			dynamicIterations.put(key, -1);
		}
	}

	@Override
	public boolean hasNext() {
		if (publishFirstEmpty) {
			return true;
		}
		if (isEmpty) {
			return false;
		}
		return position == -1 || nextAvailablePosition() != -1;
	}

	@Override
	public ParameterSet<T> next() {
		if (publishFirstEmpty) {
			publishFirstEmpty = false;
			return ParameterSet.empty();
		}
		if (isEmpty) {
			throw new NoSuchElementException();
		}
		if (position == -1) {
			// The first initialization of the values
			resetValuesFrom(0);
		} else {
			Iterator<T> it = iterators.get(references.get(position));
			if (it.hasNext()) {
				// Just advance the current iterator
				ParameterReference reference = references.get(position);
				increment(reference);
				elements.set(reference, it.next());
			} else {
				// Advance the next iterator, and reset (nextPosition, size)
				position--;
				int nextPosition = nextAvailablePosition();
				if (nextPosition == -1) {
					throw new NoSuchElementException();
				}
				ParameterReference nextReference = references.get(nextPosition);
				elements.set(nextReference, iterators.get(nextReference).next());
				increment(nextReference);
				resetValuesFrom(nextPosition + 1);
			}
		}
		return elements.copy();
	}

	public int directIteration() {
		return directIteration;
	}

	public Map<String, Integer> dynamicIterations() {
		return new HashMap<>(dynamicIterations);
	}

	private void increment(ParameterReference reference) {
		if (reference instanceof ParameterReference.Direct) {
			directIteration++;
		} else if (reference instanceof ParameterReference.Dynamic) {
			String key = ((ParameterReference.Dynamic) reference).getKey();

			dynamicIterations.computeIfPresent(key, (k, i) -> i + 1);
		}
	}

	private void resetValuesFrom(int startPosition) {
		ParameterSet<Iterator<T>> iterators = this.iterators;
		ParameterSet<Iterable<T>> iterables = this.iterables;
		ParameterSet<T> elements = this.elements;
		// In the initial reset, we can reuse the existing iterators
		// It might slightly optimize the behavior, and it supports Stream#iterator which can't be executed twice
		boolean initialReset = position == -1;
		for (int i = startPosition; i < references.size(); i++) {
			ParameterReference reference = references.get(i);
			Iterator<T> newIt = initialReset ? iterators.get(reference) : iterables.get(reference).iterator();
			iterators.set(reference, newIt);
			increment(reference);
			elements.set(reference, newIt.next());
		}
		position = references.size() - 1;
	}

	private int nextAvailablePosition() {
		ParameterSet<Iterator<T>> iterators = this.iterators;
		for (int i = position; i >= 0; i--) {
			if (iterators.get(references.get(i)).hasNext()) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * <p>
	 * Injects a dynamic property and returns the value it would have had with the current iteration.
	 * <h1>
	 * Example
	 * <p>
	 * With an iteration combining three iterators that all produce [A, B, C]:
	 * <pre>
	 * - A, A, A
	 * - A, A, B
	 * - A, A, C
	 * - A, B, A
	 * - A, B, B
	 * - A, B, C
	 * - A, C, A
	 * - A, C, B
	 * </pre>
	 * <p>
	 * The combination [A, C, B] triggers the registration of a new dynamic property that produces [X, Y, Z].
	 * The dynamic property is added, and the position is reset.
	 * The next iterations are now:
	 * <p>
	 * <pre>
	 * - A, C, B, X
	 * - A, C, B, Y
	 * - A, C, B, Z
	 * - A, C, C, X
	 * - A, C, C, Y
	 * - A, C, C, Z
	 * - B, A, A, X
	 * - B, A, A, Y
	 * - B, A, A, Z
	 * - ...
	 * </pre>
	 * <p>
	 *
	 * @param active Whether the dynamic was discovered as a result of this iterator. If false, null is returned.
	 */
	public T pushDynamic(String name, Iterable<T> iterable, boolean active) {
		Iterator<T> iterator = iterable.iterator();

		if (!iterator.hasNext()) {
			isEmpty = true;
			publishFirstEmpty = false;

			throw new NoSuchElementException();
		} else if (iterators.isEmpty()) {
			isEmpty = false;
			publishFirstEmpty = false;
		}

		ParameterReference reference = new ParameterReference.Dynamic(name);

		iterables.set(reference, iterable);
		iterators.set(reference, iterator);
		dynamicIterations.put(name, active ? 0 : -1);

		references.add(reference);

		if (position == -1) return null;

		// Setting the position without executing 'resetValuesFrom' will "inject" this iterable and then continue
		// with normal iteration, including the added iterable.
		position = references.size() - 1;

		if (!active) return null;

		T current = iterator.next();

		elements.set(reference, current);

		return current;
	}
}
