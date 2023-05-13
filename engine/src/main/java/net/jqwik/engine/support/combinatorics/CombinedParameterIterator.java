package net.jqwik.engine.support.combinatorics;

import net.jqwik.api.parameters.ParameterReference;
import net.jqwik.api.parameters.ParameterSet;

import java.util.*;

public class CombinedParameterIterator<T> implements Iterator<ParameterSet<T>> {
	private final List<ParameterReference> references;
	private final ParameterSet<Iterable<T>> iterables;
	private final ParameterSet<Iterator<T>> iterators;
	private final ParameterSet<T> elements;
	private final boolean isEmpty;
	private int position = -1;

	public CombinedParameterIterator(ParameterSet<Iterable<T>> iterables) {
		this.iterables = iterables;
		references = iterables.references();
		elements = iterables.map(i -> null);
		iterators = iterables.map(Iterable::iterator);
		isEmpty = iterables.isEmpty() || !iterators.all().stream().allMatch(Iterator::hasNext);
	}

	@Override
	public boolean hasNext() {
		if (isEmpty) {
			return false;
		}
		return position == -1 || nextAvailablePosition() != -1;
	}

	@Override
	public ParameterSet<T> next() {
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
				elements.set(references.get(position), it.next());
			} else {
				// Advance the next iterator, and reset (nextPosition, size)
				position--;
				int nextPosition = nextAvailablePosition();
				if (nextPosition == -1) {
					throw new NoSuchElementException();
				}
				ParameterReference nextReference = references.get(nextPosition);
				elements.set(nextReference, iterators.get(nextReference).next());
				resetValuesFrom(nextPosition + 1);
			}
		}
		return elements.copy();
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
}
