package com.shouyun.copperization.registry;

import com.shouyun.copperization.Copperization;
import com.shouyun.copperization.copper.CopperStatueManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.WeatheringCopperCollection;

public final class ModCreativeTabs {
	public static final ResourceKey<CreativeModeTab> COPPERIZATION_TAB_KEY = ResourceKey.create(
		Registries.CREATIVE_MODE_TAB, Copperization.id("copperization"));
	public static CreativeModeTab COPPERIZATION_TAB;

	private ModCreativeTabs() {
	}

	public static void register() {
		COPPERIZATION_TAB = Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			COPPERIZATION_TAB_KEY,
			FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.copperization.copperization"))
				.icon(() -> new ItemStack(ModItems.COPPERIZATION_WAND))
				.displayItems((parameters, output) -> creativeContents().forEach(output::accept))
				.build()
		);
	}

	public static List<ItemStack> creativeContents() {
		List<ItemStack> contents = new ArrayList<>(94);
		contents.add(new ItemStack(ModItems.COPPERIZATION_WAND));
		contents.add(new ItemStack(ModItems.RESTORATION_WAND));
		contents.add(CopperStatueManager.createCreativeSample(EntityTypes.ZOMBIE));
		contents.add(CopperStatueManager.createCreativeSample(EntityTypes.SKELETON));
		contents.add(CopperStatueManager.createCreativeSample(EntityTypes.CREEPER));
		contents.add(CopperStatueManager.createCreativeSample(EntityTypes.VILLAGER));

		addStage(contents, false, WeatheringCopperCollection.ByState::unaffected);
		addStage(contents, false, WeatheringCopperCollection.ByState::exposed);
		addStage(contents, false, WeatheringCopperCollection.ByState::weathered);
		addStage(contents, false, WeatheringCopperCollection.ByState::oxidized);
		addStage(contents, true, WeatheringCopperCollection.ByState::unaffected);
		addStage(contents, true, WeatheringCopperCollection.ByState::exposed);
		addStage(contents, true, WeatheringCopperCollection.ByState::weathered);
		addStage(contents, true, WeatheringCopperCollection.ByState::oxidized);
		return List.copyOf(contents);
	}

	private static void addStage(
		List<ItemStack> contents,
		boolean waxed,
		Function<WeatheringCopperCollection.ByState<Item>, Item> stage
	) {
		for (var family : ModBlocks.families()) {
			var collection = waxed ? family.items().waxed() : family.items().weathering();
			contents.add(new ItemStack(stage.apply(collection)));
		}
	}
}
