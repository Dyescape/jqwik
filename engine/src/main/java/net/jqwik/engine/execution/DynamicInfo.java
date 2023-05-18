package net.jqwik.engine.execution;

import java.io.Serializable;

public class DynamicInfo implements Serializable {
	private final int introductionIndex;
	private final int progress;

	public DynamicInfo(int introductionIndex, int progress) {
		this.introductionIndex = introductionIndex;
		this.progress = progress;
	}

	public int introductionIndex() {
		return introductionIndex;
	}

	public int progress() {
		return progress;
	}
}
