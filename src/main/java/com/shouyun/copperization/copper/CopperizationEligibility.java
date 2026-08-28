package com.shouyun.copperization.copper;

import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class CopperizationEligibility {
	private CopperizationEligibility() {
	}

	public static boolean canCopperize(LivingEntity entity) {
		return !(entity instanceof Player)
			&& entity.getType() != EntityTypes.ENDER_DRAGON
			&& entity.getType() != EntityTypes.WITHER
			&& entity.getType().canSerialize()
			&& entity.isAlive();
	}
}
