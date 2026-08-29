package com.shouyun.copperization.copper;

import com.shouyun.copperization.CopperizationConstants;
import com.shouyun.copperization.data.CopperStatueData;
import com.shouyun.copperization.registry.ModDataComponents;
import com.shouyun.copperization.registry.ModItems;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.EntityHitResult;

public final class CopperStatueManager {
	private static final java.util.Set<UUID> CAPTURE_LOCKS = ConcurrentHashMap.newKeySet();
	private static final Map<UUID, MiningProgress> MINING_PROGRESS = new ConcurrentHashMap<>();
	private static final DustParticleOptions MINING_PARTICLE = new DustParticleOptions(0xD47C45, 0.75F);

	private CopperStatueManager() {
	}

	public static void registerEvents() {
		UseEntityCallback.EVENT.register(CopperStatueManager::interact);
		AttackEntityCallback.EVENT.register(CopperStatueManager::attack);
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> MINING_PROGRESS.remove(entity.getUUID()));
	}

	private static InteractionResult interact(Player player, Level level, InteractionHand hand, Entity target, EntityHitResult hit) {
		if (!(target instanceof LivingEntity statue) || !CopperizationManager.isStatue(statue)) {
			return InteractionResult.PASS;
		}
		ItemStack held = player.getItemInHand(hand);
		if (level.isClientSide()) {
			return held.is(Items.HONEYCOMB) || held.is(ItemTags.AXES)
				? InteractionResult.SUCCESS : InteractionResult.PASS;
		}

		ServerLevel serverLevel = (ServerLevel) level;
		if (held.is(Items.HONEYCOMB)) return wax(serverLevel, player, hand, statue, held);
		if (held.is(ItemTags.AXES)) return scrape(serverLevel, player, hand, statue, held);
		return InteractionResult.PASS;
	}

	private static InteractionResult attack(Player player, Level level, InteractionHand hand, Entity target, EntityHitResult hit) {
		if (!(target instanceof LivingEntity statue) || !CopperizationManager.isStatue(statue)) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return player.isSpectator() ? InteractionResult.FAIL : InteractionResult.SUCCESS;
		}
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.FAIL;
		}

		MiningResult result = tryMineStatue(serverLevel, serverPlayer, statue);
		return result == MiningResult.PROGRESSED || result == MiningResult.CAPTURED
			? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
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

	public static MiningResult tryMineStatue(ServerLevel level, Player player, LivingEntity statue) {
		if (!(player instanceof ServerPlayer serverPlayer) || statue.isRemoved() || statue.level() != level || !CopperizationManager.isStatue(statue)
			|| serverPlayer.isSpectator() || !serverPlayer.mayInteract(level, statue.blockPosition())) {
			return MiningResult.BLOCKED;
		}

		ItemStack held = serverPlayer.getMainHandItem();
		if (!held.is(ItemTags.PICKAXES) || CAPTURE_LOCKS.contains(statue.getUUID())) {
			return MiningResult.BLOCKED;
		}

		if (serverPlayer.getAbilities().instabuild) {
			return capture(level, serverPlayer, statue, held, false) ? MiningResult.CAPTURED : MiningResult.BLOCKED;
		}

		UUID uuid = statue.getUUID();
		long gameTime = level.getGameTime();
		MiningProgress previous = MINING_PROGRESS.get(uuid);
		float current = previous != null && gameTime - previous.lastHitGameTime() < CopperizationConstants.STATUE_MINING_PROGRESS_TIMEOUT_TICKS
			? previous.progress() : 0.0F;
		float next = Math.min(1.0F, current + miningProgressPerHit(serverPlayer, held));
		playMiningFeedback(level, statue);
		if (next + CopperizationConstants.COPPERIZATION_COMPLETION_EPSILON < 1.0F) {
			MINING_PROGRESS.put(uuid, new MiningProgress(next, gameTime));
			return MiningResult.PROGRESSED;
		}

		return capture(level, serverPlayer, statue, held, true) ? MiningResult.CAPTURED : MiningResult.BLOCKED;
	}

	public static float miningProgressPerHit(Player player, ItemStack held) {
		var referenceState = Blocks.COPPER_BLOCK.weathering().unaffected().defaultBlockState();
		float baseSpeed = held.getDestroySpeed(referenceState);
		if (baseSpeed <= 0.0F) return 0.0F;
		float effectiveSpeed = Math.max(0.0F, player.getDestroySpeed(referenceState));
		float baseProgress = Math.min(
			CopperizationConstants.STATUE_MINING_MAX_PROGRESS_PER_HIT,
			CopperizationConstants.STATUE_MINING_BASE_PROGRESS + baseSpeed / CopperizationConstants.STATUE_MINING_SPEED_DIVISOR
		);
		return Mth.clamp(
			baseProgress * effectiveSpeed / baseSpeed,
			CopperizationConstants.STATUE_MINING_MIN_PROGRESS_PER_HIT,
			CopperizationConstants.STATUE_MINING_MAX_PROGRESS_PER_HIT
		);
	}

	private static boolean capture(ServerLevel level, ServerPlayer player, LivingEntity statue, ItemStack held, boolean createDrop) {
		UUID uuid = statue.getUUID();
		if (!CAPTURE_LOCKS.add(uuid) || statue.isRemoved() || !CopperizationManager.isStatue(statue)) return false;
		boolean captured = false;
		try {
			ItemStack result = createDrop ? createStatueStack(statue) : ItemStack.EMPTY;
			if (createDrop && !player.addItem(result) && player.drop(result, false) == null) {
				return false;
			}
			statue.ejectPassengers();
			statue.stopRiding();
			if (statue instanceof Mob mob) mob.dropLeash();
			statue.discard();
			captured = true;
			MINING_PROGRESS.remove(uuid);
			if (createDrop) held.hurtAndBreak(1, player, InteractionHand.MAIN_HAND);
			level.playSound(null, statue.blockPosition(), SoundEvents.COPPER_GOLEM_STATUE_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
			return true;
		} finally {
			if (captured) level.getServer().execute(() -> CAPTURE_LOCKS.remove(uuid));
			else CAPTURE_LOCKS.remove(uuid);
		}
	}

	private static void playMiningFeedback(ServerLevel level, LivingEntity statue) {
		level.playSound(null, statue.blockPosition(), SoundEvents.COPPER_HIT, SoundSource.PLAYERS, 0.8F, 0.9F + statue.getRandom().nextFloat() * 0.15F);
		level.sendParticles(MINING_PARTICLE, statue.getX(), statue.getY(0.55D), statue.getZ(), 4,
			statue.getBbWidth() * 0.35D, statue.getBbHeight() * 0.25D, statue.getBbWidth() * 0.35D, 0.01D);
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

	public static ItemStack createCreativeSample(EntityType<? extends LivingEntity> entityType) {
		ItemStack stack = new ItemStack(ModItems.COPPER_STATUE);
		stack.set(ModDataComponents.COPPER_STATUE_DATA, new CopperStatueData(
			BuiltInRegistries.ENTITY_TYPE.getKey(entityType), new CompoundTag(), CopperizationState.EMPTY));
		return stack;
	}

	private static CompoundTag sanitize(CompoundTag original) {
		CompoundTag tag = original.copy();
		String[] unsafe = {"UUID", "Pos", "Motion", "Rotation", "Passengers", "Leash", "leash", "HurtTime", "DeathTime", "PortalCooldown", "fall_distance", "Fire", "HasVisualFire"};
		for (String key : unsafe) tag.remove(key);
		return tag;
	}

	public enum MiningResult {
		BLOCKED,
		PROGRESSED,
		CAPTURED
	}

	private record MiningProgress(float progress, long lastHitGameTime) {
	}
}
