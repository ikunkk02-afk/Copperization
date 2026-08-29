package com.shouyun.copperization.block;

import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public final class WeatheringCopperTrapDoorBlock extends TrapDoorBlock implements WeatheringCopper {
	private final WeatherState weatherState;

	public WeatheringCopperTrapDoorBlock(WeatherState weatherState, BlockSetType type, BlockBehaviour.Properties properties) {
		super(type, properties);
		this.weatherState = weatherState;
	}

	@Override public WeatherState getAge() { return weatherState; }
}
