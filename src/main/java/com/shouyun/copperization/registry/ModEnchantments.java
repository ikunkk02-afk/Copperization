package com.shouyun.copperization.registry;

import com.shouyun.copperization.Copperization;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

public final class ModEnchantments {
	public static final ResourceKey<Enchantment> COPPERIZATION = ResourceKey.create(
		Registries.ENCHANTMENT, Copperization.id("copperization")
	);

	private ModEnchantments() {
	}
}
