package net.createmod.catnip.impl.network;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundSimpleActionPacket(String action, String value) implements CustomPacketPayload {
	public static final StreamCodec<ByteBuf, ClientboundSimpleActionPacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, ClientboundSimpleActionPacket::action,
		ByteBufCodecs.STRING_UTF8, ClientboundSimpleActionPacket::value,
		ClientboundSimpleActionPacket::new
	);

	public static final Map<String, Supplier<Consumer<String>>> ACTIONS = new HashMap<>();

	public static void addAction(String name, Supplier<Consumer<String>> action) {
		ACTIONS.put(name, action);
	}

	static {
		addAction("test", () -> System.out::println);
		addAction("configScreen", () -> SimpleCatnipActions::configScreen);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return CatnipPayloads.SIMPLE_ACTION;
	}
}
