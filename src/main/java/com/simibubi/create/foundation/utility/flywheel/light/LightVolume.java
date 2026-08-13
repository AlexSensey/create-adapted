package com.simibubi.create.foundation.utility.flywheel.light;

import com.simibubi.create.foundation.utility.flywheel.box.Box;
import com.simibubi.create.foundation.utility.flywheel.box.MutableBox;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;

public class LightVolume implements Box {
	protected final BlockAndTintGetter level;
	protected final MutableBox box = new MutableBox();
	protected boolean deleted;

	public LightVolume(BlockAndTintGetter level, Box sampleVolume) {
		this.level = level;
		this.box.assign(sampleVolume);
	}

	public Box getVolume() {
		return box;
	}

	@Override
	public int getMinX() {
		return box.getMinX();
	}

	@Override
	public int getMinY() {
		return box.getMinY();
	}

	@Override
	public int getMinZ() {
		return box.getMinZ();
	}

	@Override
	public int getMaxX() {
		return box.getMaxX();
	}

	@Override
	public int getMaxY() {
		return box.getMaxY();
	}

	@Override
	public int getMaxZ() {
		return box.getMaxZ();
	}

	public boolean isInvalid() {
		return deleted;
	}

	public short getPackedLight(int x, int y, int z) {
		return 0;
	}

	public void move(Box newSampleVolume) {
		box.assign(newSampleVolume);
	}

	public void initialize() {
	}

	public void delete() {
		deleted = true;
	}

	public void copyLight(Box levelVolume) {
	}

	public void copyBlock(Box levelVolume) {
	}

	public void copySky(Box levelVolume) {
	}

	public void onLightUpdate(LightLayer type, SectionPos pos) {
	}
}
