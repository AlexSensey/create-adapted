package net.minecraft;

/** Compatibility alias for libraries written before IdentifierException replaced it. */
public class ResourceLocationException extends IllegalArgumentException {
	public ResourceLocationException(String message) {
		super(message);
	}

	public ResourceLocationException(String message, Throwable cause) {
		super(message, cause);
	}
}
