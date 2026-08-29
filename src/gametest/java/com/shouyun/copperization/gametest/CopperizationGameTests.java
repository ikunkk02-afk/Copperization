package com.shouyun.copperization.gametest;

import com.shouyun.copperization.block.CopperizableBlockRegistry;
import com.shouyun.copperization.copper.CopperOxidationManager;
import com.shouyun.copperization.copper.CopperStatueManager;
import com.shouyun.copperization.copper.CopperizationManager;
import com.shouyun.copperization.copper.CopperizationState;
import com.shouyun.copperization.registry.ModBlocks;
import com.shouyun.copperization.registry.ModCreativeTabs;
import com.shouyun.copperization.registry.ModDataComponents;
import com.shouyun.copperization.registry.ModEnchantments;
import com.shouyun.copperization.registry.ModItems;
import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class CopperizationGameTests implements CustomTestMethodInvoker {
	@GameTest
	public void blockMappingsAreCompleteAndSafe(GameTestHelper context) {
		if (CopperizableBlockRegistry.mappings().size() != 11) throw new AssertionError("Expected 11 source mappings");
		if (ModBlocks.families().stream().mapToInt(f -> f.blocks().asList().size()).sum() != 88) throw new AssertionError("Expected 88 variants");
		if (CopperizableBlockRegistry.copperize(Blocks.CHEST.defaultBlockState()).isPresent()) throw new AssertionError("Chest must not be copperizable");
		if (CopperizableBlockRegistry.copperize(Blocks.FURNACE.defaultBlockState()).isPresent()) throw new AssertionError("Furnace must not be copperizable");
		if (CopperizableBlockRegistry.copperize(Blocks.STONE.defaultBlockState()).isEmpty()) throw new AssertionError("Stone must be copperizable");
		context.succeed();
	}

	@GameTest
	public void enchantmentOnlySupportsCopperSword(GameTestHelper context) {
		var enchantment = context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ModEnchantments.COPPERIZATION).value();
		if (!enchantment.isSupportedItem(new ItemStack(Items.COPPER_SWORD))) throw new AssertionError("Copper sword should be supported");
		if (enchantment.isSupportedItem(new ItemStack(Items.DIAMOND_SWORD))) throw new AssertionError("Diamond sword must not be supported");
		if (enchantment.definition().maxLevel() != 3) throw new AssertionError("Expected max level 3");
		context.succeed();
	}

	@GameTest(maxTicks = 200)
	public void enchantedCopperSwordHitStartsAutomaticCopperization(GameTestHelper context) {
		var player = context.makeMockServerPlayer(GameType.SURVIVAL);
		var horse = context.spawn(EntityTypes.HORSE, 1, 2, 1);
		var enchantment = context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ModEnchantments.COPPERIZATION);
		ItemStack sword = new ItemStack(Items.COPPER_SWORD);
		EnchantmentHelper.updateEnchantments(sword, mutable -> mutable.set(enchantment, 2));
		player.setItemInHand(InteractionHand.MAIN_HAND, sword);
		player.attack(horse);
		CopperizationState started = CopperizationManager.getState(horse);
		if (!started.copperizationActive() || started.copperizationLevel() != 2 || started.copperizationProgress() != 0.0F) {
			throw new AssertionError("A hit must start level II copperization without adding a progress chunk: " + started);
		}
		context.runAfterDelay(170, () -> {
			if (!CopperizationManager.isStatue(horse)) throw new AssertionError("Horse did not automatically finish after one hit");
			context.succeed();
		});
	}

	@GameTest
	public void strongerHitUpgradesSpeedWithoutResettingProgress(GameTestHelper context) {
		var cow = context.spawn(EntityTypes.COW, 1, 2, 1);
		CopperizationManager.setProgress(cow, 0.35F);
		CopperizationManager.startCopperization(cow, 1);
		CopperizationManager.startCopperization(cow, 3);
		CopperizationState upgraded = CopperizationManager.getState(cow);
		if (Math.abs(upgraded.copperizationProgress() - 0.35F) > 0.0001F || upgraded.copperizationLevel() != 3) {
			throw new AssertionError("A stronger hit reset progress instead of only upgrading speed: " + upgraded);
		}
		context.succeed();
	}

	@GameTest(maxTicks = 130)
	public void vanillaLivingEntitiesCopperizeIndependently(GameTestHelper context) {
		var entities = java.util.List.of(
			context.spawn(EntityTypes.HORSE, 1, 2, 1),
			context.spawn(EntityTypes.ZOMBIE, 2, 2, 1),
			context.spawn(EntityTypes.SKELETON, 3, 2, 1),
			context.spawn(EntityTypes.CREEPER, 4, 2, 1),
			context.spawn(EntityTypes.COW, 5, 2, 1),
			context.spawn(EntityTypes.VILLAGER, 6, 2, 1)
		);
		entities.forEach(entity -> CopperizationManager.startCopperization(entity, 3));
		context.runAfterDelay(110, () -> {
			for (var entity : entities) {
				if (!CopperizationManager.isStatue(entity)) {
					throw new AssertionError(entity.getType() + " did not independently reach statue state");
				}
			}
			context.succeed();
		});
	}

	@GameTest
	public void attachmentSurvivesEntitySaveAndLoad(GameTestHelper context) {
		var original = context.spawn(EntityTypes.COW, 1, 2, 1);
		CopperizationManager.setProgress(original, 0.50F);
		CopperizationManager.startCopperization(original, 2);
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, context.getLevel().registryAccess());
		original.saveWithoutId(output);
		var restored = EntityTypes.COW.create(context.getLevel(), net.minecraft.world.entity.EntitySpawnReason.LOAD);
		if (restored == null) throw new AssertionError("Could not create restored cow");
		restored.load(TagValueInput.create(ProblemReporter.DISCARDING, context.getLevel().registryAccess(), output.buildResult()));
		if (Math.abs(CopperizationManager.getState(restored).copperizationProgress() - 0.50F) > 0.0001F) {
			throw new AssertionError("Persistent attachment was not restored");
		}
		if (!CopperizationManager.getState(restored).copperizationActive() || CopperizationManager.getState(restored).copperizationLevel() != 2) {
			throw new AssertionError("Active state or copperization level was not restored");
		}
		context.succeed();
	}

	@GameTest(maxTicks = 40)
	public void statueFreezesAndPreservesEquipment(GameTestHelper context) {
		var zombie = context.spawn(EntityTypes.ZOMBIE, 1, 2, 1);
		zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		CopperizationManager.setProgress(zombie, 1.0F);
		double x = zombie.getX();
		if (!CopperizationManager.isStatue(zombie) || !zombie.isNoAi()) throw new AssertionError("Zombie was not frozen");
		ItemStack statue = CopperStatueManager.createStatueStack(zombie);
		if (statue.get(ModDataComponents.COPPER_STATUE_DATA) == null) throw new AssertionError("Missing statue component");
		if (!zombie.getMainHandItem().is(Items.DIAMOND_SWORD)) throw new AssertionError("Equipment was lost");
		context.runAfterDelay(5, () -> {
			if (zombie.getX() != x) throw new AssertionError("Statue moved while frozen");
			context.succeed();
		});
	}

	@GameTest
	public void waxAndScrapeOrderMatchesCopper(GameTestHelper context) {
		CopperizationState waxed = CopperizationState.EMPTY.asStatue(
			com.shouyun.copperization.copper.FrozenPoseSnapshot.capture(context.spawn(EntityTypes.COW, 1, 2, 1)), 0L
		).withOxidation(1.0F, 0L).withWaxed(true);
		CopperizationState unwaxed = CopperOxidationManager.scrape(waxed).orElseThrow();
		if (unwaxed.waxed() || unwaxed.oxidationProgress() != 1.0F) throw new AssertionError("Wax must be removed first");
		CopperizationState scraped = CopperOxidationManager.scrape(unwaxed).orElseThrow();
		if (scraped.weatherState() != net.minecraft.world.level.block.WeatheringCopper.WeatherState.WEATHERED) throw new AssertionError("Oxidation did not step back");
		context.succeed();
	}

	@GameTest
	public void statueInteractionsAreAtomicAndPlacementRestoresData(GameTestHelper context) {
		var player = context.makeMockServerPlayer(GameType.SURVIVAL);
		var zombie = context.spawn(EntityTypes.ZOMBIE, 1, 2, 1);
		zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		CopperizationManager.setProgress(zombie, 1.0F);

		ItemStack honeycomb = new ItemStack(Items.HONEYCOMB, 2);
		player.setItemInHand(InteractionHand.MAIN_HAND, honeycomb);
		UseEntityCallback.EVENT.invoker().interact(player, context.getLevel(), InteractionHand.MAIN_HAND, zombie, new EntityHitResult(zombie));
		if (!CopperizationManager.getState(zombie).waxed() || honeycomb.getCount() != 1) throw new AssertionError("Waxing failed");

		ItemStack axe = new ItemStack(Items.IRON_AXE);
		player.setItemInHand(InteractionHand.MAIN_HAND, axe);
		UseEntityCallback.EVENT.invoker().interact(player, context.getLevel(), InteractionHand.MAIN_HAND, zombie, new EntityHitResult(zombie));
		if (CopperizationManager.getState(zombie).waxed() || axe.getDamageValue() != 1) throw new AssertionError("Wax scraping failed");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
		UseEntityCallback.EVENT.invoker().interact(player, context.getLevel(), InteractionHand.MAIN_HAND, zombie, new EntityHitResult(zombie));
		if (zombie.isRemoved()) throw new AssertionError("Pickaxe right-click still captured the statue");
		if (countItem(player, ModItems.COPPER_STATUE) != 0) throw new AssertionError("Pickaxe right-click produced a statue item");

		player.setOnGround(true);
		CopperStatueManager.tryMineStatue(context.getLevel(), player, zombie);
		CopperStatueManager.tryMineStatue(context.getLevel(), player, zombie);
		if (zombie.isRemoved()) throw new AssertionError("Iron pickaxe captured the statue before the third hit");
		CopperStatueManager.tryMineStatue(context.getLevel(), player, zombie);
		if (!zombie.isRemoved()) throw new AssertionError("Iron pickaxe did not capture the statue on the third hit");
		ItemStack statueStack = findItem(player, ModItems.COPPER_STATUE);
		if (statueStack.isEmpty() || statueStack.getCount() != 1) throw new AssertionError("Capture did not produce exactly one statue item");
		CopperStatueManager.tryMineStatue(context.getLevel(), player, zombie);
		if (countItem(player, ModItems.COPPER_STATUE) != 1) throw new AssertionError("Second capture duplicated the statue");

		BlockPos ground = new BlockPos(3, 1, 3);
		context.setBlock(ground, Blocks.STONE);
		player.setItemInHand(InteractionHand.MAIN_HAND, statueStack);
		ModItems.COPPER_STATUE.useOn(useContext(context, player, ground));
		if (!statueStack.isEmpty()) throw new AssertionError("Survival placement did not consume the item");
		var placed = context.getEntities(EntityTypes.ZOMBIE).stream().filter(entity -> !entity.isRemoved()).findFirst().orElseThrow();
		if (!CopperizationManager.isStatue(placed) || !placed.getMainHandItem().is(Items.DIAMOND_SWORD)) {
			throw new AssertionError("Placed statue did not restore statue state and equipment");
		}
		context.succeed();
	}

	@GameTest
	public void statueMiningRequiresPickaxesAndUsesVanillaToolSpeed(GameTestHelper context) {
		var player = context.makeMockServerPlayer(GameType.SURVIVAL);
		player.setOnGround(true);
		var blocked = context.spawn(EntityTypes.ZOMBIE, 1, 2, 1);
		CopperizationManager.setProgress(blocked, 1.0F);
		for (var item : List.of(Items.AIR, Items.IRON_SWORD, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE)) {
			player.setItemInHand(InteractionHand.MAIN_HAND, item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item));
			if (CopperStatueManager.tryMineStatue(context.getLevel(), player, blocked) != CopperStatueManager.MiningResult.BLOCKED) {
				throw new AssertionError(item + " unexpectedly mined a statue");
			}
			if (blocked.isRemoved()) throw new AssertionError(item + " removed a statue");
		}
		blocked.discard();

		List<net.minecraft.world.item.Item> pickaxes = List.of(
			Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.COPPER_PICKAXE, Items.IRON_PICKAXE,
			Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE
		);
		int[] expectedHits = {5, 4, 3, 3, 2, 2, 2};
		for (int index = 0; index < pickaxes.size(); index++) {
			ItemStack tool = new ItemStack(pickaxes.get(index));
			player.setItemInHand(InteractionHand.MAIN_HAND, tool);
			player.setOnGround(true);
			var statue = context.spawn(EntityTypes.COW, 1, 2, 1);
			CopperizationManager.setProgress(statue, 1.0F);
			for (int hit = 1; hit < expectedHits[index]; hit++) {
				if (CopperStatueManager.tryMineStatue(context.getLevel(), player, statue) != CopperStatueManager.MiningResult.PROGRESSED) {
					throw new AssertionError(pickaxes.get(index) + " did not progress on hit " + hit);
				}
				if (statue.isRemoved()) throw new AssertionError(pickaxes.get(index) + " captured too early");
			}
			if (CopperStatueManager.tryMineStatue(context.getLevel(), player, statue) != CopperStatueManager.MiningResult.CAPTURED || !statue.isRemoved()) {
				throw new AssertionError(pickaxes.get(index) + " did not capture in " + expectedHits[index] + " hits");
			}
			if (tool.getDamageValue() != 1) throw new AssertionError(pickaxes.get(index) + " durability cost was not exactly one");
		}
		ItemStack modifierProbe = new ItemStack(Items.WOODEN_PICKAXE);
		player.setOnGround(true);
		float groundedProgress = CopperStatueManager.miningProgressPerHit(player, modifierProbe);
		player.setOnGround(false);
		float airborneProgress = CopperStatueManager.miningProgressPerHit(player, modifierProbe);
		if (!(airborneProgress < groundedProgress)) {
			throw new AssertionError("Vanilla airborne mining penalty did not reduce statue mining progress");
		}
		context.succeed();
	}

	@GameTest(maxTicks = 80)
	public void statueMiningProgressExpiresAndCreativeDoesNotDrop(GameTestHelper context) {
		var survival = context.makeMockServerPlayer(GameType.SURVIVAL);
		survival.setOnGround(true);
		ItemStack woodenPickaxe = new ItemStack(Items.WOODEN_PICKAXE);
		survival.setItemInHand(InteractionHand.MAIN_HAND, woodenPickaxe);
		var statue = context.spawn(EntityTypes.COW, 1, 2, 1);
		CopperizationManager.setProgress(statue, 1.0F);
		CopperStatueManager.tryMineStatue(context.getLevel(), survival, statue);
		context.runAfterDelay(41, () -> {
			for (int hit = 0; hit < 4; hit++) CopperStatueManager.tryMineStatue(context.getLevel(), survival, statue);
			if (statue.isRemoved()) throw new AssertionError("Mining progress did not reset after two seconds");
			CopperStatueManager.tryMineStatue(context.getLevel(), survival, statue);
			if (!statue.isRemoved()) throw new AssertionError("Wooden pickaxe did not capture after five new hits");

			var creative = context.makeMockServerPlayer(GameType.CREATIVE);
			ItemStack creativePickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
			creative.setItemInHand(InteractionHand.MAIN_HAND, creativePickaxe);
			var creativeStatue = context.spawn(EntityTypes.COW, 2, 2, 1);
			CopperizationManager.setProgress(creativeStatue, 1.0F);
			int before = countItem(creative, ModItems.COPPER_STATUE);
			if (CopperStatueManager.tryMineStatue(context.getLevel(), creative, creativeStatue) != CopperStatueManager.MiningResult.CAPTURED) {
				throw new AssertionError("Creative pickaxe did not instantly remove the statue");
			}
			if (countItem(creative, ModItems.COPPER_STATUE) != before || creativePickaxe.getDamageValue() != 0) {
				throw new AssertionError("Creative statue mining created a drop or damaged the tool");
			}
			context.succeed();
		});
	}

	@GameTest
	public void statueMiningProgressIsSharedWithoutDuplication(GameTestHelper context) {
		var first = context.makeMockServerPlayer(GameType.SURVIVAL);
		var second = context.makeMockServerPlayer(GameType.SURVIVAL);
		first.setOnGround(true);
		second.setOnGround(true);
		first.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
		second.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
		var statue = context.spawn(EntityTypes.ZOMBIE, 1, 2, 1);
		CopperizationManager.setProgress(statue, 1.0F);

		if (CopperStatueManager.tryMineStatue(context.getLevel(), first, statue) != CopperStatueManager.MiningResult.PROGRESSED) {
			throw new AssertionError("First player did not contribute shared progress");
		}
		if (CopperStatueManager.tryMineStatue(context.getLevel(), second, statue) != CopperStatueManager.MiningResult.CAPTURED) {
			throw new AssertionError("Second player did not complete shared progress");
		}
		CopperStatueManager.tryMineStatue(context.getLevel(), first, statue);
		int total = countItem(first, ModItems.COPPER_STATUE) + countItem(second, ModItems.COPPER_STATUE);
		if (total != 1) throw new AssertionError("Concurrent capture produced " + total + " statue items");
		context.succeed();
	}

	@GameTest
	public void completedStatuesClearFireAndRejectAllIgnition(GameTestHelper context) {
		var zombie = context.spawn(EntityTypes.ZOMBIE, 1, 2, 1);
		CopperizationManager.setProgress(zombie, 0.50F);
		zombie.igniteForSeconds(5.0F);
		zombie.setSharedFlagOnFire(true);
		if (!zombie.isOnFire()) throw new AssertionError("Partially copperized zombie should still burn");

		CopperizationManager.setProgress(zombie, 1.0F);
		assertNotBurning(zombie, "Copperization completion did not clear fire");
		zombie.igniteForSeconds(5.0F);
		assertNotBurning(zombie, "igniteForSeconds reignited a statue");
		zombie.setRemainingFireTicks(100);
		assertNotBurning(zombie, "setRemainingFireTicks reignited a statue");
		zombie.lavaIgnite();
		assertNotBurning(zombie, "Lava reignited a statue");

		var player = context.makeMockServerPlayer(GameType.SURVIVAL);
		ItemStack fireAspectSword = new ItemStack(Items.IRON_SWORD);
		var fireAspect = context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FIRE_ASPECT);
		EnchantmentHelper.updateEnchantments(fireAspectSword, mutable -> mutable.set(fireAspect, 2));
		player.setItemInHand(InteractionHand.MAIN_HAND, fireAspectSword);
		player.attack(zombie);
		assertNotBurning(zombie, "Fire Aspect reignited a statue");
		context.succeed();
	}

	@GameTest(maxTicks = 240)
	public void sunlightStopsBurningExactlyWhenZombieBecomesAStatue(GameTestHelper context) {
		var server = context.getLevel().getServer();
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set day");
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
		var zombie = context.spawn(EntityTypes.ZOMBIE, 1, 2, 1);
		CopperizationManager.setProgress(zombie, 0.50F);
		context.startSequence()
			.thenWaitUntil(() -> context.assertTrue(zombie.isOnFire(), "Partially copperized zombie has not burned in sunlight yet"))
			.thenExecute(() -> {
				CopperizationManager.setProgress(zombie, 1.0F);
				assertNotBurning(zombie, "Sunlit zombie kept burning when copperization completed");
			})
			.thenIdle(20)
			.thenExecute(() -> assertNotBurning(zombie, "Completed zombie statue reignited in sunlight"))
			.thenSucceed();
	}

	@GameTest
	public void loadedStatuesClearLegacyFireState(GameTestHelper context) {
		var original = context.spawn(EntityTypes.ZOMBIE, 1, 2, 1);
		CopperizationManager.setProgress(original, 1.0F);
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, context.getLevel().registryAccess());
		original.saveWithoutId(output);
		var tag = output.buildResult();
		tag.remove("UUID");
		tag.putShort("Fire", (short)100);
		original.discard();

		var restored = EntityTypes.ZOMBIE.create(context.getLevel(), net.minecraft.world.entity.EntitySpawnReason.LOAD);
		if (restored == null) throw new AssertionError("Could not create restored zombie statue");
		restored.load(TagValueInput.create(ProblemReporter.DISCARDING, context.getLevel().registryAccess(), tag));
		if (!context.getLevel().addFreshEntity(restored)) throw new AssertionError("Could not load restored zombie statue");
		assertNotBurning(restored, "Loaded statue retained legacy fire state");
		context.succeed();
	}

	@GameTest
	public void copperizationCreativeTabHasOrderedUsableContents(GameTestHelper context) {
		if (ModCreativeTabs.COPPERIZATION_TAB == null || !ModCreativeTabs.COPPERIZATION_TAB.getIconItem().is(ModItems.COPPERIZATION_WAND)) {
			throw new AssertionError("Copperization creative tab or icon was not registered");
		}
		List<ItemStack> contents = ModCreativeTabs.creativeContents();
		if (contents.size() != 93 || !contents.getFirst().is(ModItems.COPPERIZATION_WAND)) {
			throw new AssertionError("Unexpected creative tab size or first item: " + contents.size());
		}

		var sampleTypes = List.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.CREEPER, EntityTypes.VILLAGER);
		for (int index = 0; index < sampleTypes.size(); index++) {
			ItemStack sample = contents.get(index + 1);
			var data = sample.get(ModDataComponents.COPPER_STATUE_DATA);
			if (!sample.is(ModItems.COPPER_STATUE) || data == null
				|| !data.entityType().equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(sampleTypes.get(index)))) {
				throw new AssertionError("Creative statue sample " + index + " is blank or has the wrong entity type");
			}
		}
		var samplePlayer = context.makeMockServerPlayer(GameType.SURVIVAL);
		var placedSample = placeStatueStack(context, samplePlayer, EntityTypes.ZOMBIE, contents.get(1).copy(), new BlockPos(1, 1, 3));
		if (!CopperizationManager.isStatue(placedSample)) {
			throw new AssertionError("Creative statue sample could not be placed as a usable statue");
		}

		for (int stage = 0; stage < 8; stage++) {
			boolean waxed = stage >= 4;
			int weatherStage = stage % 4;
			for (int familyIndex = 0; familyIndex < ModBlocks.families().size(); familyIndex++) {
				var family = ModBlocks.families().get(familyIndex);
				var byState = waxed ? family.items().waxed() : family.items().weathering();
				var expected = switch (weatherStage) {
					case 0 -> byState.unaffected();
					case 1 -> byState.exposed();
					case 2 -> byState.weathered();
					default -> byState.oxidized();
				};
				ItemStack actual = contents.get(5 + stage * ModBlocks.families().size() + familyIndex);
				if (!actual.is(expected)) throw new AssertionError("Creative block ordering mismatch at stage " + stage + ", family " + family.name());
			}
		}
		context.succeed();
	}

	@GameTest
	public void statueItemRoundTripsRichEntityData(GameTestHelper context) {
		var player = context.makeMockServerPlayer(GameType.SURVIVAL);

		var zombie = context.spawn(EntityTypes.ZOMBIE, 1, 2, 1);
		zombie.setBaby(true);
		zombie.setCustomName(Component.literal("Copper Guard"));
		zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		CopperizationManager.setProgress(zombie, 1.0F);
		CopperizationManager.setState(zombie, CopperizationManager.getState(zombie).withOxidation(0.75F, 99L).withWaxed(true));
		ItemStack zombieStack = CopperStatueManager.createStatueStack(zombie);
		zombie.discard();
		var placedZombie = placeStatueStack(context, player, EntityTypes.ZOMBIE, zombieStack, new BlockPos(1, 1, 3));
		if (!placedZombie.isBaby() || !placedZombie.getMainHandItem().is(Items.DIAMOND_SWORD)
			|| placedZombie.getCustomName() == null || !placedZombie.getCustomName().getString().equals("Copper Guard")) {
			throw new AssertionError("Zombie age, equipment, or custom name did not round-trip");
		}
		CopperizationState zombieState = CopperizationManager.getState(placedZombie);
		if (!zombieState.waxed() || zombieState.weatherState() != net.minecraft.world.level.block.WeatheringCopper.WeatherState.WEATHERED
			|| zombieState.frozenPose().isEmpty()) {
			throw new AssertionError("Zombie statue state did not round-trip");
		}

		var skeleton = context.spawn(EntityTypes.SKELETON, 2, 2, 1);
		skeleton.setCustomName(Component.literal("Shield Bearer"));
		skeleton.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
		CopperizationManager.setProgress(skeleton, 1.0F);
		ItemStack skeletonStack = CopperStatueManager.createStatueStack(skeleton);
		skeleton.discard();
		var placedSkeleton = placeStatueStack(context, player, EntityTypes.SKELETON, skeletonStack, new BlockPos(2, 1, 3));
		if (!placedSkeleton.getOffhandItem().is(Items.SHIELD) || placedSkeleton.getCustomName() == null
			|| !placedSkeleton.getCustomName().getString().equals("Shield Bearer")) {
			throw new AssertionError("Skeleton equipment or custom name did not round-trip");
		}

		var horse = context.spawn(EntityTypes.HORSE, 3, 2, 1);
		horse.setCustomName(Component.literal("Verdigris"));
		var horseVariant = horse.getVariant();
		var horseMarkings = horse.getMarkings();
		CopperizationManager.setProgress(horse, 1.0F);
		ItemStack horseStack = CopperStatueManager.createStatueStack(horse);
		horse.discard();
		var placedHorse = placeStatueStack(context, player, EntityTypes.HORSE, horseStack, new BlockPos(3, 1, 3));
		if (placedHorse.getVariant() != horseVariant || placedHorse.getMarkings() != horseMarkings
			|| placedHorse.getCustomName() == null || !placedHorse.getCustomName().getString().equals("Verdigris")) {
			throw new AssertionError("Horse variant, markings, or custom name did not round-trip");
		}

		var villager = context.spawn(EntityTypes.VILLAGER, 4, 2, 1);
		villager.setCustomName(Component.literal("Copper Smith"));
		villager.setVillagerData(villager.getVillagerData().withLevel(3));
		var villagerData = villager.getVillagerData();
		CopperizationManager.setProgress(villager, 1.0F);
		ItemStack villagerStack = CopperStatueManager.createStatueStack(villager);
		villager.discard();
		var placedVillager = placeStatueStack(context, player, EntityTypes.VILLAGER, villagerStack, new BlockPos(4, 1, 3));
		if (!placedVillager.getVillagerData().equals(villagerData) || placedVillager.getCustomName() == null
			|| !placedVillager.getCustomName().getString().equals("Copper Smith")) {
			throw new AssertionError("Villager data or custom name did not round-trip");
		}
		context.succeed();
	}

	@GameTest
	public void wandTransformsOnlyMappedBlocksAndUsesDurability(GameTestHelper context) {
		var player = context.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		ItemStack wand = new ItemStack(ModItems.COPPERIZATION_WAND);
		player.setItemInHand(InteractionHand.MAIN_HAND, wand);
		BlockPos stone = new BlockPos(1, 1, 1);
		context.setBlock(stone, Blocks.STONE);
		ModItems.COPPERIZATION_WAND.useOn(useContext(context, player, stone));
		if (context.getBlockState(stone).is(Blocks.STONE)) throw new AssertionError("Wand did not transform stone");
		if (wand.getDamageValue() != 1 || !player.getCooldowns().isOnCooldown(wand)) {
			throw new AssertionError("Wand cost or cooldown was not applied (damage=" + wand.getDamageValue()
				+ ", cooldown=" + player.getCooldowns().isOnCooldown(wand) + ")");
		}

		BlockPos chest = new BlockPos(2, 1, 1);
		context.setBlock(chest, Blocks.CHEST);
		ModItems.COPPERIZATION_WAND.useOn(useContext(context, player, chest));
		if (!context.getBlockState(chest).is(Blocks.CHEST)) throw new AssertionError("Wand modified an unsupported block");
		context.succeed();
	}

	private static ItemStack findItem(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.Item item) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) return stack;
		}
		return ItemStack.EMPTY;
	}

	private static int countItem(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.Item item) {
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(item)) count += stack.getCount();
		}
		return count;
	}

	private static void assertNotBurning(net.minecraft.world.entity.LivingEntity entity, String message) {
		if (entity.isOnFire() || entity.getRemainingFireTicks() > 0 || entity.displayFireAnimation()) {
			throw new AssertionError(message + " (ticks=" + entity.getRemainingFireTicks() + ")");
		}
	}

	private static <T extends net.minecraft.world.entity.LivingEntity> T placeStatueStack(
		GameTestHelper context,
		net.minecraft.world.entity.player.Player player,
		net.minecraft.world.entity.EntityType<T> type,
		ItemStack stack,
		BlockPos ground
	) {
		context.setBlock(ground, Blocks.STONE);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		ModItems.COPPER_STATUE.useOn(useContext(context, player, ground));
		if (!stack.isEmpty()) throw new AssertionError("Survival statue placement did not consume " + type);
		return context.getEntities(type).stream().filter(entity -> !entity.isRemoved()).findFirst()
			.orElseThrow(() -> new AssertionError("Placed statue was missing for " + type));
	}

	private static UseOnContext useContext(GameTestHelper context, net.minecraft.world.entity.player.Player player, BlockPos relativePos) {
		BlockPos absolutePos = context.absolutePos(relativePos);
		return new UseOnContext(player, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(absolutePos), net.minecraft.core.Direction.UP, absolutePos, false));
	}

	@Override
	public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
		context.setBlock(new BlockPos(0, 0, 0), Blocks.STONE);
		method.invoke(this, context);
	}
}
