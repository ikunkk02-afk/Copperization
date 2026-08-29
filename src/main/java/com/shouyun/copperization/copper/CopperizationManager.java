package com.shouyun.copperization.copper;

import com.shouyun.copperization.Copperization;
import com.shouyun.copperization.CopperizationConstants;
import com.shouyun.copperization.registry.ModAttachments;
import com.shouyun.copperization.registry.ModEnchantments;
import java.util.IdentityHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class CopperizationManager {
	private static final Identifier MOVEMENT_MODIFIER = Copperization.id("copperization_movement");
	private static final Identifier ATTACK_DAMAGE_MODIFIER = Copperization.id("copperization_attack_damage");
	private static final Identifier ATTACK_SPEED_MODIFIER = Copperization.id("copperization_attack_speed");
	private static final DustParticleOptions FRESH_COPPER_PARTICLE = new DustParticleOptions(0xD47C45, 1.0F);
	private static final DustParticleOptions OXIDIZED_COPPER_PARTICLE = new DustParticleOptions(0x5FAF91, 0.8F);
	private static final Map<LivingEntity, Float> LIVE_PROGRESS = new IdentityHashMap<>();

	private CopperizationManager() {
	}

	public static void registerEvents() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register(CopperizationManager::afterDamage);
		ServerEntityEvents.ENTITY_LOAD.register(CopperizationManager::onEntityLoad);
		ServerEntityEvents.ENTITY_UNLOAD.register(CopperizationManager::onEntityUnload);
	}

	private static void afterDamage(LivingEntity target, DamageSource source, float baseDamage, float damageTaken, boolean blocked) {
		if (damageTaken <= 0.0F || blocked || !(source.getEntity() instanceof Player player)
			|| source.getDirectEntity() != player || !CopperizationEligibility.canCopperize(target)) {
			return;
		}

		ItemStack weapon = player.getMainHandItem();
		if (!weapon.is(Items.COPPER_SWORD) || !(target.level() instanceof ServerLevel level)) return;

		var enchantment = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
			.getOrThrow(ModEnchantments.COPPERIZATION);
		int enchantmentLevel = EnchantmentHelper.getItemEnchantmentLevel(enchantment, weapon);
		if (enchantmentLevel > 0) startCopperization(target, enchantmentLevel);
	}

	public static CopperizationState getState(LivingEntity entity) {
		return entity.getAttachedOrElse(ModAttachments.COPPERIZATION_STATE, CopperizationState.EMPTY);
	}

	public static boolean isStatue(Entity entity) {
		return entity instanceof LivingEntity living
			&& living.hasAttached(ModAttachments.COPPERIZATION_STATE)
			&& getState(living).copperStatue();
	}

	public static void setState(LivingEntity entity, CopperizationState state) {
		entity.setAttached(ModAttachments.COPPERIZATION_STATE, state);
	}

	public static void startCopperization(LivingEntity entity, int enchantmentLevel) {
		if (!(entity.level() instanceof ServerLevel level) || enchantmentLevel <= 0 || !CopperizationEligibility.canCopperize(entity)) return;

		CopperizationState oldState = getState(entity);
		if (oldState.copperStatue()) return;

		int levelValue = Math.clamp(enchantmentLevel, 1, 3);
		if (!oldState.copperizationActive()) {
			CopperizationState started = oldState.start(levelValue);
			LIVE_PROGRESS.put(entity, started.copperizationProgress());
			setState(entity, started);
			applyGameplayState(entity, started);
			playStartFeedback(level, entity);
		} else if (levelValue > oldState.copperizationLevel()) {
			setState(entity, oldState.withLevel(levelValue));
		}
	}

	public static void tickCopperization(ServerLevel level, LivingEntity entity) {
		if (!entity.hasAttached(ModAttachments.COPPERIZATION_STATE)) return;
		CopperizationState state = getState(entity);
		if (!state.copperizationActive() || state.copperStatue()) return;

		int copperizationLevel = Math.max(1, state.copperizationLevel());
		float current = LIVE_PROGRESS.getOrDefault(entity, state.copperizationProgress());
		float next = Math.min(CopperizationConstants.STATUE_THRESHOLD,
			current + CopperizationConstants.progressPerTick(copperizationLevel));
		if (next + CopperizationConstants.COPPERIZATION_COMPLETION_EPSILON >= CopperizationConstants.STATUE_THRESHOLD) {
			completeCopperization(level, entity, state);
			return;
		}

		LIVE_PROGRESS.put(entity, next);
		CopperizationState liveState = state.withProgress(next);
		applyGameplayState(entity, liveState);
		if (entity.tickCount % CopperizationConstants.COPPERIZATION_SYNC_INTERVAL_TICKS == 0) setState(entity, liveState);
		if (entity.tickCount % CopperizationConstants.COPPERIZATION_PARTICLE_INTERVAL_TICKS == 0) spawnProgressParticle(level, entity, next);
	}

	/** Used by commands and automated render/game tests without reintroducing hit-based accumulation. */
	public static void setProgress(LivingEntity entity, float progress) {
		if (!(entity.level() instanceof ServerLevel level) || !CopperizationEligibility.canCopperize(entity)) return;
		CopperizationState state = getState(entity).withProgress(progress);
		if (progress >= CopperizationConstants.STATUE_THRESHOLD) {
			completeCopperization(level, entity, state);
		} else {
			if (state.copperizationActive()) LIVE_PROGRESS.put(entity, state.copperizationProgress());
			else LIVE_PROGRESS.remove(entity);
			setState(entity, state);
			applyGameplayState(entity, state);
		}
	}

	private static void completeCopperization(ServerLevel level, LivingEntity entity, CopperizationState state) {
		CopperizationState statue = state.withProgress(1.0F).asStatue(
			FrozenPoseSnapshot.capture(entity), level.getGameTime() + CopperizationConstants.OXIDATION_SAMPLE_INTERVAL);
		LIVE_PROGRESS.remove(entity);
		setState(entity, statue);
		applyGameplayState(entity, statue);
		level.playSound(null, entity.blockPosition(), SoundEvents.COPPER_GOLEM_BECOME_STATUE, SoundSource.NEUTRAL, 0.9F, 1.0F);
	}

	public static void applyGameplayState(LivingEntity entity, CopperizationState state) {
		float progress = state.copperizationProgress();
		updateModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_MODIFIER, CopperizationConstants.movementModifier(progress));
		updateModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_DAMAGE_MODIFIER, CopperizationConstants.combatModifier(progress));
		updateModifier(entity.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_MODIFIER, CopperizationConstants.combatModifier(progress));
		if (state.copperStatue()) freeze(entity, state);
	}

	private static void updateModifier(AttributeInstance attribute, Identifier id, double amount) {
		if (attribute == null) return;
		attribute.removeModifier(id);
		if (amount != 0.0D) attribute.addOrUpdateTransientModifier(
			new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}

	public static void freeze(LivingEntity entity, CopperizationState state) {
		state.frozenPose().ifPresent(snapshot -> snapshot.applyTransform(entity));
		entity.setDeltaMovement(0.0D, 0.0D, 0.0D);
		entity.setNoGravity(true);
		entity.setInvulnerable(true);
		entity.fallDistance = 0.0F;
		if (entity instanceof Mob mob) {
			mob.setNoAi(true);
			mob.setTarget(null);
			mob.getNavigation().stop();
		}
	}

	private static void onEntityLoad(Entity entity, ServerLevel level) {
		if (entity instanceof LivingEntity living && living.hasAttached(ModAttachments.COPPERIZATION_STATE)) {
			CopperizationState state = getState(living);
			if (!state.copperStatue() && state.copperizationProgress() > 0.0F && !state.copperizationActive()) {
				state = state.start(Math.max(1, state.copperizationLevel()));
				setState(living, state);
			}
			if (state.copperizationActive()) LIVE_PROGRESS.put(living, state.copperizationProgress());
			applyGameplayState(living, state);
		}
	}

	private static void onEntityUnload(Entity entity, ServerLevel level) {
		if (!(entity instanceof LivingEntity living)) return;
		Float progress = LIVE_PROGRESS.remove(living);
		if (progress != null && living.hasAttached(ModAttachments.COPPERIZATION_STATE)) {
			setState(living, getState(living).withProgress(progress));
		}
	}

	private static void playStartFeedback(ServerLevel level, LivingEntity entity) {
		level.playSound(null, entity.blockPosition(), SoundEvents.COPPER_HIT, SoundSource.PLAYERS, 1.0F, 1.15F);
		level.sendParticles(FRESH_COPPER_PARTICLE, entity.getX(), entity.getY(0.55D), entity.getZ(), 14,
			entity.getBbWidth() * 0.45D, entity.getBbHeight() * 0.35D, entity.getBbWidth() * 0.45D, 0.02D);
	}

	private static void spawnProgressParticle(ServerLevel level, LivingEntity entity, float progress) {
		DustParticleOptions particle = progress >= CopperizationConstants.HEAVY_THRESHOLD && entity.tickCount % 40 == 0
			? OXIDIZED_COPPER_PARTICLE : FRESH_COPPER_PARTICLE;
		level.sendParticles(particle, entity.getX(), entity.getRandomY(), entity.getZ(), 1,
			entity.getBbWidth() * 0.35D, 0.1D, entity.getBbWidth() * 0.35D, 0.0D);
	}
}
