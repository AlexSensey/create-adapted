package net.createmod.catnip.api.lang;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class LangBuilder {
	private MutableComponent component = Component.empty();
	private final String namespace;

	public LangBuilder(String namespace) {
		this.namespace = namespace;
	}

	public static Object[] resolveBuilders(Object[] args) {
		Object[] resolved = new Object[args.length];
		for (int i = 0; i < args.length; i++)
			resolved[i] = args[i] instanceof LangBuilder builder ? builder.component() : args[i];
		return resolved;
	}

	public LangBuilder text(String text) {
		component.append(text);
		return this;
	}

	public LangBuilder text(ChatFormatting formatting, String text) {
		component.append(Component.literal(text).withStyle(formatting));
		return this;
	}

	public LangBuilder space() {
		return text(" ");
	}

	public LangBuilder translate(String key, Object... args) {
		component.append(Component.translatable(namespace + "." + key, resolveBuilders(args)));
		return this;
	}

	public LangBuilder add(Component component) {
		this.component.append(component);
		return this;
	}

	public LangBuilder add(LangBuilder builder) {
		return add(builder.component());
	}

	public LangBuilder style(ChatFormatting... styles) {
		component.withStyle(styles);
		return this;
	}

	/**
	 * Kept as a distinct overload for binary compatibility with Ponder and Catnip.
	 */
	public LangBuilder style(ChatFormatting style) {
		component.withStyle(style);
		return this;
	}

	public MutableComponent component() {
		return component;
	}

	/**
	 * Binary-compatible text accessor used by Ponder screens.
	 */
	public String string() {
		return component.getString();
	}

	public void forGoggles(List<Component> tooltip) {
		forGoggles(tooltip, 0);
	}

	public void forGoggles(List<Component> tooltip, int indent) {
		tooltip.add(component.copy());
	}
}
