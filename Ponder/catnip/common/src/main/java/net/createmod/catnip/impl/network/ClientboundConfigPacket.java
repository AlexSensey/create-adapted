package net.createmod.catnip.impl.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientboundConfigPacket(String path, String value) implements CustomPacketPayload {
	public static final StreamCodec<ByteBuf, ClientboundConfigPacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, ClientboundConfigPacket::path,
		ByteBufCodecs.STRING_UTF8, ClientboundConfigPacket::value,
		ClientboundConfigPacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return CatnipPayloads.CLIENTBOUND_CONFIG;
	}
}
