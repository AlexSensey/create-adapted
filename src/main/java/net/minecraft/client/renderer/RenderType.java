package net.minecraft.client.renderer;

/**
 * TODO 26.2: compatibility shim for libraries still compiled against the old
 * RenderType package.
 */
public final class RenderType {
	private final net.minecraft.client.renderer.rendertype.RenderType delegate;

	private RenderType(net.minecraft.client.renderer.rendertype.RenderType delegate) {
		this.delegate = delegate;
	}

	public static RenderType solid() {
		return new RenderType(null);
	}

	public static RenderType cutout() {
		return new RenderType(null);
	}

	public static RenderType cutoutMipped() {
		return new RenderType(null);
	}

	public static RenderType translucent() {
		return new RenderType(null);
	}

	public net.minecraft.client.renderer.rendertype.RenderType unwrap() {
		return delegate;
	}
}
