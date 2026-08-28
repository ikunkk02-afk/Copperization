package com.shouyun.copperization.gametest;

import com.shouyun.copperization.block.CopperizableBlockRegistry;
import com.shouyun.copperization.copper.CopperOxidationManager;
import com.shouyun.copperization.copper.CopperStatueManager;
import com.shouyun.copperization.copper.CopperizationManager;
import com.shouyun.copperization.copper.CopperizationState;
import com.shouyun.copperization.registry.ModBlocks;
import com.shouyun.copperization.registry.ModDataComponents;
import com.shouyun.copperization.registry.ModEnchantments;
import com.shouyun.copperization.registry.ModItems;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
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

	@GameTest
	public void enchantedCopperSwordHitAddsProgress(GameTestHelper context) {
		var player = context.makeMockServerPlayer(GameType.SURVIVAL);
		var zombie = context.spawn(EntityTypes.ZOMBIE, 1, 2, 1);
		var enchantment = context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ModEnchantments.COPPERIZATION);
		ItemStack sword = new ItemStack(Items.COPPER_SWORD);
		EnchantmentHelper.updateEnchantments(sword, mutable -> mutable.set(enchantment, 2));
		player.setItemInHand(InteractionHand.MAIN_HAND, sword);
		player.attack(zombie);
		float progress = CopperizationManager.getState(zombie).copperizationProgress();
		if (Math.abs(progress - 0.22F) > 0.0001F) throw new AssertionError("Expected level II hit to add 22%, got " + progress);
		context.succeed();
	}

	@GameTest
	public void attachmentSurvivesEntitySaveAndLoad(GameTestHelper context) {
		var original = context.spawn(EntityTypes.COW, 1, 2, 1);
		CopperizationManager.addProgress(original, 0.50F);
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, context.getLevel().registryAccess());
		original.saveWithoutId(output);
		var restored = EntityTypes.COW.create(context.getLevel(), net.minecraft.world.entity.EntitySpawnReason.LOAD);
		if (restored == null) throw new AssertionError("Could not create restored cow");
		restored.load(TagValueInput.create(ProblemReporter.DISCARDING, context.getLevel().registryAccess(), output.buildResult()));
		if (Math.abs(CopperizationManager.getState(restored).copperizationProgress() - 0.50F) > 0.0001F) {
			throw new AssertionError("Persistent attachment was not restored");
		}
		context.succeed();
	}

	@GameTest(maxTicks = 40)
	public void statueFreezesAndPreservesEquipment(GameTestHelper context) {
		var zombie = context.spawn(EntityTypes.ZOMBIE, 1, 2, 1);
		zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		CopperizationManager.addProgress(zombie, 1.0F);
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
		CopperizationManager.addProgress(zombie, 1.0F);

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
		if (!zombie.isRemoved()) throw new AssertionError("Statue was not captured");
		ItemStack statueStack = findItem(player, ModItems.COPPER_STATUE);
		if (statueStack.isEmpty() || statueStack.getCount() != 1) throw new AssertionError("Capture did not produce exactly one statue item");
		UseEntityCallback.EVENT.invoker().interact(player, context.getLevel(), InteractionHand.MAIN_HAND, zombie, new EntityHitResult(zombie));
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
