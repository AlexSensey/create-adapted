package net.minecraft.resources;

public record ResourceLocation(String namespace, String path) implements Comparable<ResourceLocation> {
	public static final char NAMESPACE_SEPARATOR = ':';

	public static ResourceLocation fromNamespaceAndPath(String namespace, String path) {
		return new ResourceLocation(namespace, path);
	}

	public static ResourceLocation parse(String value) {
		int separator = value.indexOf(':');
		return separator >= 0
			? new ResourceLocation(value.substring(0, separator), value.substring(separator + 1))
			: new ResourceLocation(Identifier.DEFAULT_NAMESPACE, value);
	}

	public static ResourceLocation withDefaultNamespace(String path) {
		return new ResourceLocation(Identifier.DEFAULT_NAMESPACE, path);
	}

	public static ResourceLocation fromIdentifier(Identifier identifier) {
		return new ResourceLocation(identifier.getNamespace(), identifier.getPath());
	}

	public String getNamespace() {
		return namespace;
	}

	public String getPath() {
		return path;
	}

	public Identifier asIdentifier() {
		return Identifier.fromNamespaceAndPath(namespace, path);
	}

	public ResourceLocation withPrefix(String prefix) {
		return new ResourceLocation(namespace, prefix + path);
	}

	public static boolean isAllowedInResourceLocation(char character) {
		return character == NAMESPACE_SEPARATOR || character == '/' || character == '_' || character == '-'
			|| character == '.' || character >= '0' && character <= '9'
			|| character >= 'a' && character <= 'z';
	}

	public String toDebugFileName() {
		return namespace + '_' + path.replace('/', '_');
	}

	@Override
	public String toString() {
		return namespace + ":" + path;
	}

	@Override
	public int compareTo(ResourceLocation other) {
		return toString().compareTo(other.toString());
	}
}
