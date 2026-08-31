package net.createmod.catnip.net.base;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.createmod.catnip.api.network.NetworkHelper;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class CatnipPacketRegistry {
	private static final List<PacketType<?>> PACKETS = new ArrayList<>();

	private final String namespace;
	private final String version;

	public CatnipPacketRegistry(String namespace, String version) {
		this.namespace = namespace;
		this.version = version;
	}

	public void registerPacket(PacketType<?> type) {
		PACKETS.add(type.withRegistryInfo(namespace, version));
	}

	public void registerAllPackets() {
		for (PacketType<?> packet : PACKETS) {
			registerCodec(packet);
		}
	}

	public static void register(RegisterPayloadHandlersEvent event) {
		if (PACKETS.isEmpty())
			return;
		PacketType<?> first = PACKETS.getFirst();
		PayloadRegistrar registrar = event.registrar(first.namespace)
			.versioned(first.version);
		for (PacketType<?> packet : PACKETS) {
			register(registrar, packet);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static <T extends BasePacketPayload> void register(PayloadRegistrar registrar, PacketType<T> packet) {
		if (ClientboundPacketPayload.class.isAssignableFrom(packet.clazz())) {
			registrar.playToClient(packet.type(), packet.codec());
		}
		if (ServerboundPacketPayload.class.isAssignableFrom(packet.clazz())) {
			registrar.playToServer(packet.type(), packet.codec(), CatnipPacketRegistry::handle);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static <T extends BasePacketPayload> void registerCodec(PacketType<T> packet) {
		if (ClientboundPacketPayload.class.isAssignableFrom(packet.clazz())) {
			NetworkHelper.INSTANCE.clientboundCodecs().register((CustomPacketPayload.Type) packet.type(), packet.codec());
		}
		if (ServerboundPacketPayload.class.isAssignableFrom(packet.clazz())) {
			NetworkHelper.INSTANCE.serverboundCodecs().register((CustomPacketPayload.Type) packet.type(), packet.codec());
			NetworkHelper.INSTANCE.registerPayloadHandler((CustomPacketPayload.Type) packet.type(), (payload, player) -> {
				if (payload instanceof ServerboundPacketPayload serverbound)
					serverbound.handle(player);
			});
		}
	}

	private static void handle(BasePacketPayload payload, IPayloadContext context) {
		if (payload instanceof ServerboundPacketPayload serverbound)
			handleServerbound(serverbound, context);
	}

	private static void handleServerbound(ServerboundPacketPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Player player = context.player();
			if (player instanceof ServerPlayer serverPlayer)
				payload.handle(serverPlayer);
		});
	}

	public record PacketType<T extends BasePacketPayload>(
		CustomPacketPayload.Type<T> type,
		Class<T> clazz,
		StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
		String namespace,
		String version
	) {
		public PacketType(CustomPacketPayload.Type<T> type, Class<T> clazz, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
			this(type, clazz, codec, type.id().getNamespace(), "");
		}

		private PacketType<T> withRegistryInfo(String namespace, String version) {
			return new PacketType<>(type, clazz, codec, namespace, version);
		}
	}
}
