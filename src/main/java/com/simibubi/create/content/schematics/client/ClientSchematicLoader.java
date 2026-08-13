package com.simibubi.create.content.schematics.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.simibubi.create.Create;
import com.simibubi.create.content.schematics.packet.SchematicUploadPacket;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.CreatePaths;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public class ClientSchematicLoader {

	public static final int PACKET_DELAY = 10;

	private final List<Component> availableSchematics;
	private final Map<String, InputStream> activeUploads;
	private int packetCycle;

	public ClientSchematicLoader() {
		availableSchematics = new ArrayList<>();
		activeUploads = new HashMap<>();
		refresh();
	}

	public void tick() {
		if (activeUploads.isEmpty())
			return;
		if (packetCycle-- > 0)
			return;
		packetCycle = PACKET_DELAY;

		for (String schematic : new HashSet<>(activeUploads.keySet()))
			continueUpload(schematic);
	}

	public void startNewUpload(String schematic) {
		Path path = CreatePaths.SCHEMATICS_DIR.resolve(schematic);

		if (!Files.exists(path)) {
			Create.LOGGER.error("Missing Schematic file: {}", path);
			return;
		}

		try {
			long size = Files.size(path);
			if (!validateSizeLimitation(size))
				return;

			if (!isGZIPEncoded(path.toFile())) {
				LocalPlayer player = Minecraft.getInstance().player;
				if (player != null)
					player.sendSystemMessage(CreateLang.translateDirect("schematics.wrongFormat"));
				return;
			}

			InputStream input = Files.newInputStream(path, StandardOpenOption.READ);
			InputStream previous = activeUploads.put(schematic, input);
			if (previous != null)
				previous.close();
			ClientNetworkHelper.INSTANCE.sendToServer(SchematicUploadPacket.begin(schematic, size));
		} catch (IOException e) {
			Create.LOGGER.error("Encountered an error while starting schematic upload", e);
		}
	}

	public static boolean validateSizeLimitation(long size) {
		if (Minecraft.getInstance().hasSingleplayerServer())
			return true;
		long maxSize = AllConfigs.server().schematics.maxTotalSchematicSize.get();
		if (size <= maxSize * 1000)
			return true;

		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null) {
			player.sendSystemMessage(
				CreateLang.translateDirect("schematics.uploadTooLarge").append(" (" + size / 1000 + " KB)."));
			player.sendSystemMessage(
				CreateLang.translateDirect("schematics.maxAllowedSize").append(" " + maxSize + " KB"));
		}
		return false;
	}

	/**
	 * Checks the two-byte GZIP magic number (0x1F, 0x8B).
	 */
	public static boolean isGZIPEncoded(File file) {
		try (FileInputStream input = new FileInputStream(file)) {
			byte[] bytes = new byte[2];
			if (input.read(bytes) != 2)
				return false;
			return (bytes[0] & 0xFF) == 0x1F && (bytes[1] & 0xFF) == 0x8B;
		} catch (IOException exception) {
			return false;
		}
	}

	private void continueUpload(String schematic) {
		InputStream input = activeUploads.get(schematic);
		if (input == null)
			return;

		int maxPacketSize = AllConfigs.server().schematics.maxSchematicPacketSize.get();
		byte[] data = new byte[maxPacketSize];
		try {
			int status = input.read(data);
			if (status != -1) {
				if (status < maxPacketSize)
					data = Arrays.copyOf(data, status);
				if (Minecraft.getInstance().level != null)
					ClientNetworkHelper.INSTANCE.sendToServer(SchematicUploadPacket.write(schematic, data));
				else {
					closeUpload(schematic);
					return;
				}
			}

			if (status < maxPacketSize)
				finishUpload(schematic);
		} catch (IOException e) {
			Create.LOGGER.error("Encountered an error while uploading schematic", e);
			closeUpload(schematic);
		}
	}

	private void finishUpload(String schematic) {
		if (!activeUploads.containsKey(schematic))
			return;
		ClientNetworkHelper.INSTANCE.sendToServer(SchematicUploadPacket.finish(schematic));
		closeUpload(schematic);
	}

	private void closeUpload(String schematic) {
		InputStream input = activeUploads.remove(schematic);
		if (input == null)
			return;
		try {
			input.close();
		} catch (IOException e) {
			Create.LOGGER.warn("Failed to close schematic upload stream for {}", schematic, e);
		}
	}

	public void refresh() {
		FilesHelper.createFolderIfMissing(CreatePaths.SCHEMATICS_DIR);
		availableSchematics.clear();

		try (Stream<Path> paths = Files.list(CreatePaths.SCHEMATICS_DIR)) {
			paths.filter(path -> !Files.isDirectory(path) && path.getFileName()
				.toString()
				.endsWith(".nbt"))
				.map(path -> Component.literal(path.getFileName()
					.toString()))
				.forEach(availableSchematics::add);
		} catch (NoSuchFileException ignored) {
		} catch (IOException e) {
			Create.LOGGER.error("Failed to refresh schematics", e);
		}

		availableSchematics.sort((first, second) -> compareNatural(first.getString(), second.getString()));
	}

	private static int compareNatural(String first, String second) {
		String a = first.endsWith(".nbt") ? first.substring(0, first.length() - 4) : first;
		String b = second.endsWith(".nbt") ? second.substring(0, second.length() - 4) : second;
		int aLength = a.length();
		int bLength = b.length();
		int minSize = Math.min(aLength, bLength);
		boolean numeric = false;
		int lastNumericCompare = 0;

		for (int i = 0; i < minSize; i++) {
			char aChar = a.charAt(i);
			char bChar = b.charAt(i);
			boolean aNumber = Character.isDigit(aChar);
			boolean bNumber = Character.isDigit(bChar);
			if (numeric) {
				if (aNumber && bNumber) {
					if (lastNumericCompare == 0)
						lastNumericCompare = aChar - bChar;
				} else if (aNumber)
					return 1;
				else if (bNumber)
					return -1;
				else if (lastNumericCompare == 0) {
					if (aChar != bChar)
						return aChar - bChar;
					numeric = false;
				} else
					return lastNumericCompare;
			} else if (aNumber && bNumber) {
				numeric = true;
				lastNumericCompare = aChar - bChar;
			} else if (aChar != bChar)
				return aChar - bChar;
		}

		if (!numeric)
			return aLength - bLength;
		if (aLength > bLength && Character.isDigit(a.charAt(bLength)))
			return 1;
		if (bLength > aLength && Character.isDigit(b.charAt(aLength)))
			return -1;
		return lastNumericCompare == 0 ? aLength - bLength : lastNumericCompare;
	}

	public List<Component> getAvailableSchematics() {
		return availableSchematics;
	}
}
