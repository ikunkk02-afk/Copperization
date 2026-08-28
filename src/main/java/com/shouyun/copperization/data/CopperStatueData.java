package com.shouyun.copperization.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.shouyun.copperization.copper.CopperizationState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

public record CopperStatueData(Identifier entityType, CompoundTag entityData, CopperizationState state) {
	public static final Codec<CopperStatueData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Identifier.CODEC.fieldOf("entity_type").forGetter(CopperStatueData::entityType),
		CompoundTag.CODEC.fieldOf("entity_data").forGetter(CopperStatueData::entityData),
		CopperizationState.CODEC.fieldOf("copperization_state").forGetter(CopperStatueData::state)
	).apply(instance, CopperStatueData::new));

	public CopperStatueData {
		entityData = entityData.copy();
	}

	@Override
	public CompoundTag entityData() {
		return entityData.copy();
	}
}
