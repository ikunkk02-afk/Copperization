package com.shouyun.copperization.block;

import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.WoodType;

public final class WeatheringCopperFenceGateBlock extends FenceGateBlock implements WeatheringCopper {
	private final WeatherState weatherState;

	public WeatheringCopperFenceGateBlock(WeatherState weatherState, WoodType type, BlockBehaviour.Properties properties) {
		super(type, properties);
		this.weatherState = weatherState;
	}

	@Override public WeatherState getAge() { return weatherState; }
}
