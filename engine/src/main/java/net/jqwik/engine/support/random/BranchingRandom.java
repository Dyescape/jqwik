package net.jqwik.engine.support.random;

import net.jqwik.engine.SourceOfRandomness;

import java.util.Random;

public class BranchingRandom {
	private final Random root;

	public BranchingRandom(long rootSeed) {
		this.root = SourceOfRandomness.newRandom(rootSeed);
	}

	public Random branch() {
		return SourceOfRandomness.newRandom(root.nextLong());
	}
}
