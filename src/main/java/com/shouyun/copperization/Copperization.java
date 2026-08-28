package com.shouyun.copperization;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import com.shouyun.copperization.copper.CopperizationManager;
import com.shouyun.copperization.copper.CopperStatueManager;
import com.shouyun.copperization.registry.ModAttachments;
import com.shouyun.copperization.registry.ModBlocks;
import com.shouyun.copperization.registry.ModItems;
import com.shouyun.copperization.registry.ModDataComponents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Copperization implements ModInitializer {
	public static final String MOD_ID = "copperization";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModAttachments.register();
		ModDataComponents.register();
		ModBlocks.register();
		ModItems.register();
		CopperizationManager.registerEvents();
		CopperStatueManager.registerEvents();
		LOGGER.info("Copperization 0.1.0 initialized");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
