package com.shouyun.copperization.registry;

import com.shouyun.copperization.Copperization;
import com.shouyun.copperization.data.CopperStatueData;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModDataComponents {
	public static DataComponentType<CopperStatueData> COPPER_STATUE_DATA;

	private ModDataComponents() {
	}

	public static void register() {
		COPPER_STATUE_DATA = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Copperization.id("copper_statue_data"),
			DataComponentType.<CopperStatueData>builder()
				.persistent(CopperStatueData.CODEC)
				.networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(CopperStatueData.CODEC))
				.cacheEncoding()
				.build()
		);
	}
}
