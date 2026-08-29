package com.shouyun.copperization.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Small per-position payload; the original {@code BlockState} stays in the chunk palette. */
public record CopperizedBlockData(int oxidationStage, boolean waxed, long nextOxidationTick) {
	public static final Codec<CopperizedBlockData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.intRange(0, 3).fieldOf("oxidation_stage").forGetter(CopperizedBlockData::oxidationStage),
		Codec.BOOL.fieldOf("waxed").forGetter(CopperizedBlockData::waxed),
		Codec.LONG.fieldOf("next_oxidation_tick").forGetter(CopperizedBlockData::nextOxidationTick)
	).apply(instance, CopperizedBlockData::new));

	public CopperizedBlockData {
		oxidationStage = Math.clamp(oxidationStage, 0, 3);
	}

	public static CopperizedBlockData fresh(long nextOxidationTick) {
		return new CopperizedBlockData(0, false, nextOxidationTick);
	}

	public CopperizedBlockData withWaxed(boolean value) {
		return new CopperizedBlockData(oxidationStage, value, nextOxidationTick);
	}

	public CopperizedBlockData withStage(int stage, long nextTick) {
		return new CopperizedBlockData(stage, waxed, nextTick);
	}
}
