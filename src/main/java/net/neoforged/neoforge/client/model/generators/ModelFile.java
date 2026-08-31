package net.neoforged.neoforge.client.model.generators;

public class ModelFile {
	public static class ExistingModelFile extends ModelFile {
		public ExistingModelFile(Object location, Object existingFileHelper) {
		}
	}

	public static class UncheckedModelFile extends ModelFile {
		public UncheckedModelFile(String path) {
		}
	}
}
