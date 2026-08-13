package com.simibubi.create.content.equipment.hats;

import com.simibubi.create.content.trains.schedule.hat.TrainHatInfo;

public interface CreateHatRenderState {
	int create$getHatType();

	void create$setHatType(int type);

	TrainHatInfo create$getHatInfo();

	void create$setHatInfo(TrainHatInfo info);
}
