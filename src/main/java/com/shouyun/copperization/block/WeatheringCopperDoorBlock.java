package com.shouyun.copperization.block;

import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public final class WeatheringCopperDoorBlock extends DoorBlock implements WeatheringCopper {
	private final WeatherState weatherState;

	public WeatheringCopperDoorBlock(WeatherState weatherState, BlockSetType type, BlockBehaviour.Properties properties) {
		super(type, properties);
		this.weatherState = weatherState;
	}

	@Override public WeatherState getAge() { return weatherState; }
}
