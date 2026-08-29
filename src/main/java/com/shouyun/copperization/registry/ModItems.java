package com.shouyun.copperization.registry;

import com.shouyun.copperization.Copperization;
import com.shouyun.copperization.CopperizationConstants;
import com.shouyun.copperization.item.CopperizationWandItem;
import com.shouyun.copperization.item.CopperStatueItem;
import com.shouyun.copperization.item.RestorationWandItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ModItems {
	public static Item COPPERIZATION_WAND;
	public static Item RESTORATION_WAND;
	public static Item COPPER_STATUE;

	private ModItems() {
	}

	public static void register() {
		COPPERIZATION_WAND = register("copperization_wand", properties -> new CopperizationWandItem(properties.durability(CopperizationConstants.WAND_DURABILITY)));
		RESTORATION_WAND = register("restoration_wand", properties -> new RestorationWandItem(properties.durability(CopperizationConstants.WAND_DURABILITY)));
		COPPER_STATUE = register("copper_statue", properties -> new CopperStatueItem(properties.stacksTo(1)));
	}

	private static Item register(String name, java.util.function.Function<Item.Properties, Item> factory) {
		Identifier id = Copperization.id(name);
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
	}
}
