package com.shouyun.copperization.copper;

import com.shouyun.copperization.data.CopperStatueData;
import com.shouyun.copperization.registry.ModAttachments;
import com.shouyun.copperization.registry.ModDataComponents;
import com.shouyun.copperization.registry.ModItems;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.EntityHitResult;

public final class CopperStatueManager {
	private static final Set<UUID> CAPTURE_LOCKS = ConcurrentHashMap.newKeySet();

	private CopperStatueManager() {
	}

	public static void registerEvents() {
		UseEntityCallback.EVENT.register(CopperStatueManager::interact);
	}

	private static InteractionResult interact(Player player, Level level, InteractionHand hand, Entity target, EntityHitResult hit) {
		if (!(target instanceof LivingEntity statue) || !CopperizationManager.isStatue(statue)) {
			return InteractionResult.PASS;
		}
		ItemStack held = player.getItemInHand(hand);
		if (level.isClientSide()) {
			return held.is(Items.HONEYCOMB) || held.is(ItemTags.AXES) || held.is(ItemTags.PICKAXES)
				? InteractionResult.SUCCESS : InteractionResult.PASS;
		}

		ServerLevel serverLevel = (ServerLevel) level;
		if (held.is(Items.HONEYCOMB)) return wax(serverLevel, player, hand, statue, held);
		if (held.is(ItemTags.AXES)) return scrape(serverLevel, player, hand, statue, held);
		if (held.is(ItemTags.PICKAXES)) return capture(serverLevel, player, statue);
		return InteractionResult.PASS;
	}

	private static InteractionResult wax(ServerLevel level, Player player, InteractionHand hand, LivingEntity statue, ItemStack held) {
		CopperizationState state = CopperizationManager.getState(statue);
		if (state.waxed()) return InteractionResult.PASS;
		CopperizationManager.setState(statue, state.withWaxed(true));
		if (!player.getAbilities().instabuild) held.shrink(1);
		level.levelEvent(player, LevelEvent.PARTICLES_AND_SOUND_WAX_ON, statue.blockPosition(), 0);
		return InteractionResult.SUCCESS_SERVER;
	}

	private static InteractionResult scrape(ServerLevel level, Player player, InteractionHand hand, LivingEntity statue, ItemStack held) {
		CopperizationState state = CopperizationManager.getState(statue);
		var changed = CopperOxidationManager.scrape(state);
		if (changed.isEmpty()) return InteractionResult.PASS;
		boolean removedWax = state.waxed();
		CopperizationManager.setState(statue, changed.get());
		held.hurtAndBreak(1, player, hand);
		level.playSound(null, statue.blockPosition(), removedWax ? SoundEvents.AXE_WAX_OFF : SoundEvents.AXE_SCRAPE, SoundSource.PLAYERS, 1.0F, 1.0F);
		level.levelEvent(player, removedWax ? LevelEvent.PARTICLES_WAX_OFF : LevelEvent.PARTICLES_SCRAPE, statue.blockPosition(), 0);
		return InteractionResult.SUCCESS_SERVER;
	}

	private static InteractionResult capture(ServerLevel level, Player player, LivingEntity statue) {
		UUID uuid = statue.getUUID();
		if (!CAPTURE_LOCKS.add(uuid) || statue.isRemoved()) return InteractionResult.FAIL;
		try {
			ItemStack result = createStatueStack(statue);
			if (!player.addItem(result) && player.drop(result, false) == null) {
				CAPTURE_LOCKS.remove(uuid);
				return InteractionResult.FAIL;
			}
			statue.ejectPassengers();
			statue.stopRiding();
			if (statue instanceof Mob mob) mob.dropLeash();
			level.playSound(null, statue.blockPosition(), SoundEvents.COPPER_GOLEM_STATUE_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
			statue.discard();
			level.getServer().execute(() -> CAPTURE_LOCKS.remove(uuid));
			return InteractionResult.SUCCESS_SERVER;
		} catch (RuntimeException exception) {
			CAPTURE_LOCKS.remove(uuid);
			throw exception;
		}
	}

	public static ItemStack createStatueStack(LivingEntity statue) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, statue.registryAccess());
		statue.saveWithoutId(output);
		CompoundTag tag = sanitize(output.buildResult());
		CopperStatueData data = new CopperStatueData(BuiltInRegistries.ENTITY_TYPE.getKey(statue.getType()), tag, CopperizationManager.getState(statue));
		ItemStack stack = new ItemStack(ModItems.COPPER_STATUE);
		stack.set(ModDataComponents.COPPER_STATUE_DATA, data);
		return stack;
	}

	private static CompoundTag sanitize(CompoundTag original) {
		CompoundTag tag = original.copy();
		String[] unsafe = {"UUID", "Pos", "Motion", "Rotation", "Passengers", "Leash", "leash", "HurtTime", "DeathTime", "PortalCooldown", "fall_distance", "Fire"};
		for (String key : unsafe) tag.remove(key);
		return tag;
	}
}
