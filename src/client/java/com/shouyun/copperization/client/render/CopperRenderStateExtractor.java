package com.shouyun.copperization.client.render;

import com.shouyun.copperization.CopperizationConstants;
import com.shouyun.copperization.copper.CopperizationManager;
import com.shouyun.copperization.registry.ModAttachments;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public final class CopperRenderStateExtractor {
	public static final RenderStateDataKey<CopperRenderState> KEY = RenderStateDataKey.create(() -> "copperization:living_state");
	private static final Map<LivingEntity, ProgressInterpolation> INTERPOLATIONS = new WeakHashMap<>();

	private CopperRenderStateExtractor() {
	}

	public static void extract(LivingEntity entity, LivingEntityRenderState renderState, float partialTicks) {
		FabricRenderState fabricState = (FabricRenderState) renderState;
		if (!entity.hasAttached(ModAttachments.COPPERIZATION_STATE)) {
			INTERPOLATIONS.remove(entity);
			fabricState.setData(KEY, null);
			return;
		}

		var state = CopperizationManager.getState(entity);
		if (state.copperizationProgress() <= 0.0F) {
			fabricState.setData(KEY, null);
			return;
		}

		ProgressInterpolation interpolation = INTERPOLATIONS.computeIfAbsent(entity,
			ignored -> new ProgressInterpolation(state.copperizationProgress(), state.copperizationProgress(), entity.tickCount));
		float renderTime = entity.tickCount + partialTicks;
		float previous = interpolation.value(renderTime);
		if (Math.abs(interpolation.current - state.copperizationProgress()) > 1.0E-6F) {
			interpolation.previous = previous;
			interpolation.current = state.copperizationProgress();
			interpolation.changedAtTick = entity.tickCount;
		}

		float progress = interpolation.value(renderTime);
		fabricState.setData(KEY, new CopperRenderState(progress, interpolation.previous, state.weatherState(),
			state.copperStatue(), state.waxed(), state.frozenPose()));
		if (state.copperStatue()) state.frozenPose().ifPresent(snapshot -> applyFrozenPose(renderState, snapshot));
	}

	public static void applyArmedPose(LivingEntity entity, ArmedEntityRenderState state) {
		if (!entity.hasAttached(ModAttachments.COPPERIZATION_STATE)) return;
		var copper = CopperizationManager.getState(entity);
		if (copper.copperStatue()) copper.frozenPose().ifPresent(snapshot -> state.attackTime = snapshot.attackAnimation());
	}

	private static void applyFrozenPose(LivingEntityRenderState state, com.shouyun.copperization.copper.FrozenPoseSnapshot snapshot) {
		state.bodyRot = snapshot.bodyYaw();
		state.yRot = Mth.wrapDegrees(snapshot.headYaw() - snapshot.bodyYaw());
		state.xRot = snapshot.pitch();
		state.walkAnimationPos = snapshot.limbPosition();
		state.walkAnimationSpeed = snapshot.limbSpeed();
		state.pose = snapshot.pose();
		state.ageInTicks = snapshot.ageInTicks();
		if (state instanceof ArmedEntityRenderState armed) armed.attackTime = snapshot.attackAnimation();
	}

	private static final class ProgressInterpolation {
		private float previous;
		private float current;
		private int changedAtTick;

		private ProgressInterpolation(float previous, float current, int changedAtTick) {
			this.previous = previous;
			this.current = current;
			this.changedAtTick = changedAtTick;
		}

		private float value(float renderTime) {
			float delta = Mth.clamp((renderTime - changedAtTick) / CopperizationConstants.COPPERIZATION_SYNC_INTERVAL_TICKS, 0.0F, 1.0F);
			return Mth.lerp(delta, previous, current);
		}
	}
}
