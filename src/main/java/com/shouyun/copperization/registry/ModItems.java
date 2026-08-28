package com.shouyun.copperization.registry;

import com.shouyun.copperization.Copperization;
import com.shouyun.copperization.CopperizationConstants;
import com.shouyun.copperization.item.CopperizationWandItem;
import com.shouyun.copperization.item.CopperStatueItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModItems {
	public static Item COPPERIZATION_WAND;
	public static Item COPPER_STATUE;

	private ModItems() {
	}

	public static void register() {
		COPPERIZATION_WAND = register("copperization_wand", properties -> new CopperizationWandItem(properties.durability(CopperizationConstants.WAND_DURABILITY)));
		COPPER_STATUE = register("copper_statue", properties -> new CopperStatueItem(properties.stacksTo(1)));

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
			entries.accept(COPPERIZATION_WAND);
			entries.accept(COPPER_STATUE);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> ModBlocks.families().forEach(family -> entries.accept(family.freshItem())));
	}

	private static Item register(String name, java.util.function.Function<Item.Properties, Item> factory) {
		Identifier id = Copperization.id(name);
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
	}
}
