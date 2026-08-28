package com.shouyun.copperization.copper;

import com.shouyun.copperization.Copperization;
import com.shouyun.copperization.CopperizationConstants;
import com.shouyun.copperization.registry.ModAttachments;
import com.shouyun.copperization.registry.ModEnchantments;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
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

	private CopperizationManager() {
	}

	public static void registerEvents() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register(CopperizationManager::afterDamage);
		ServerEntityEvents.ENTITY_LOAD.register(CopperizationManager::onEntityLoad);
	}

	private static void afterDamage(LivingEntity target, DamageSource source, float baseDamage, float damageTaken, boolean blocked) {
		if (damageTaken <= 0.0F || blocked || !(source.getEntity() instanceof Player player)
			|| source.getDirectEntity() != player || !CopperizationEligibility.canCopperize(target)) {
			return;
		}

		ItemStack weapon = player.getMainHandItem();
		if (!weapon.is(Items.COPPER_SWORD) || !(target.level() instanceof ServerLevel level)) {
			return;
		}

		var enchantment = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
			.getOrThrow(ModEnchantments.COPPERIZATION);
		int enchantmentLevel = EnchantmentHelper.getItemEnchantmentLevel(enchantment, weapon);
		if (enchantmentLevel > 0) {
			addProgress(target, CopperizationConstants.progressForLevel(enchantmentLevel));
		}
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

	public static void addProgress(LivingEntity entity, float amount) {
		if (!(entity.level() instanceof ServerLevel level) || amount <= 0.0F || !CopperizationEligibility.canCopperize(entity)) {
			return;
		}

		CopperizationState oldState = getState(entity);
		if (oldState.copperStatue()) return;

		float next = Math.clamp(oldState.copperizationProgress() + amount, 0.0F, 1.0F);
		CopperizationState newState = oldState.withProgress(next);
		if (next >= CopperizationConstants.STATUE_THRESHOLD) {
			newState = newState.asStatue(FrozenPoseSnapshot.capture(entity), level.getGameTime() + CopperizationConstants.OXIDATION_SAMPLE_INTERVAL);
		}

		setState(entity, newState);
		applyGameplayState(entity, newState);
	}

	public static void applyGameplayState(LivingEntity entity, CopperizationState state) {
		int stage = CopperizationConstants.debuffStage(state.copperizationProgress());
		updateModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), MOVEMENT_MODIFIER, CopperizationConstants.MOVEMENT_MULTIPLIERS[stage]);
		updateModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_DAMAGE_MODIFIER, CopperizationConstants.COMBAT_MULTIPLIERS[stage]);
		updateModifier(entity.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_MODIFIER, CopperizationConstants.COMBAT_MULTIPLIERS[stage]);

		if (state.copperStatue()) {
			freeze(entity, state);
		}
	}

	private static void updateModifier(AttributeInstance attribute, Identifier id, double amount) {
		if (attribute == null) return;
		attribute.removeModifier(id);
		if (amount != 0.0D) {
			attribute.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
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
			applyGameplayState(living, getState(living));
		}
	}
}
