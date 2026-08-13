package com.simibubi.create.compat.jei.category.sequencedAssembly;

public abstract class SequencedAssemblySubCategory {

	private final int width;

	public SequencedAssemblySubCategory(int width) {
		this.width = width;
	}

	public int getWidth() {
		return width;
	}

	public static class AssemblyPressing extends SequencedAssemblySubCategory {
		public AssemblyPressing() {
			super(25);
		}
	}

	public static class AssemblySpouting extends SequencedAssemblySubCategory {
		public AssemblySpouting() {
			super(25);
		}
	}

	public static class AssemblyDeploying extends SequencedAssemblySubCategory {
		public AssemblyDeploying() {
			super(25);
		}
	}

	public static class AssemblyCutting extends SequencedAssemblySubCategory {
		public AssemblyCutting() {
			super(25);
		}
	}

}
