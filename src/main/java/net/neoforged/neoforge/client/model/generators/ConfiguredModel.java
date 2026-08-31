package net.neoforged.neoforge.client.model.generators;

public class ConfiguredModel {
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		public Builder modelFile(ModelFile model) {
			return this;
		}

		public Builder rotationX(int value) {
			return this;
		}

		public Builder rotationY(int value) {
			return this;
		}

		public Builder uvLock(boolean value) {
			return this;
		}

		public ConfiguredModel[] build() {
			return new ConfiguredModel[] { new ConfiguredModel() };
		}

		public ConfiguredModel nextModel() {
			return new ConfiguredModel();
		}
	}
}
