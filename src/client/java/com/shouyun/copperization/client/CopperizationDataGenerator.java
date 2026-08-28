package com.shouyun.copperization.client;

import com.shouyun.copperization.client.datagen.CopperizationDataProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.data.DataProvider;

public class CopperizationDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		fabricDataGenerator.createPack().addProvider((DataProvider.Factory<CopperizationDataProvider>) CopperizationDataProvider::new);
	}
}
