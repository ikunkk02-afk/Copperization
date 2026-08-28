package com.shouyun.copperization.copper;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

public record FrozenPoseSnapshot(
	double x,
	double y,
	double z,
	float yaw,
	float pitch,
	float bodyYaw,
	float headYaw,
	Pose pose,
	float limbPosition,
	float limbSpeed,
	float attackAnimation,
	float ageInTicks,
	boolean usingItem,
	boolean usingMainHand
) {
	public static final Codec<FrozenPoseSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.DOUBLE.fieldOf("x").forGetter(FrozenPoseSnapshot::x),
		Codec.DOUBLE.fieldOf("y").forGetter(FrozenPoseSnapshot::y),
		Codec.DOUBLE.fieldOf("z").forGetter(FrozenPoseSnapshot::z),
		Codec.FLOAT.fieldOf("yaw").forGetter(FrozenPoseSnapshot::yaw),
		Codec.FLOAT.fieldOf("pitch").forGetter(FrozenPoseSnapshot::pitch),
		Codec.FLOAT.fieldOf("body_yaw").forGetter(FrozenPoseSnapshot::bodyYaw),
		Codec.FLOAT.fieldOf("head_yaw").forGetter(FrozenPoseSnapshot::headYaw),
		Pose.CODEC.fieldOf("pose").forGetter(FrozenPoseSnapshot::pose),
		Codec.FLOAT.fieldOf("limb_position").forGetter(FrozenPoseSnapshot::limbPosition),
		Codec.FLOAT.fieldOf("limb_speed").forGetter(FrozenPoseSnapshot::limbSpeed),
		Codec.FLOAT.fieldOf("attack_animation").forGetter(FrozenPoseSnapshot::attackAnimation),
		Codec.FLOAT.fieldOf("age_in_ticks").forGetter(FrozenPoseSnapshot::ageInTicks),
		Codec.BOOL.fieldOf("using_item").forGetter(FrozenPoseSnapshot::usingItem),
		Codec.BOOL.fieldOf("using_main_hand").forGetter(FrozenPoseSnapshot::usingMainHand)
	).apply(instance, FrozenPoseSnapshot::new));

	public static FrozenPoseSnapshot capture(LivingEntity entity) {
		return new FrozenPoseSnapshot(
			entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot(),
			entity.yBodyRot, entity.yHeadRot, entity.getPose(), entity.walkAnimation.position(),
			entity.walkAnimation.speed(), entity.attackAnim, entity.tickCount,
			entity.isUsingItem(), entity.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND
		);
	}

	public FrozenPoseSnapshot at(double newX, double newY, double newZ, float newYaw) {
		return new FrozenPoseSnapshot(newX, newY, newZ, newYaw, pitch, newYaw, newYaw, pose,
			limbPosition, limbSpeed, attackAnimation, ageInTicks, usingItem, usingMainHand);
	}

	public void applyTransform(LivingEntity entity) {
		entity.setPos(x, y, z);
		entity.setYRot(yaw);
		entity.setXRot(pitch);
		entity.setYBodyRot(bodyYaw);
		entity.setYHeadRot(headYaw);
		entity.setPose(pose);
		entity.setDeltaMovement(0.0D, 0.0D, 0.0D);
	}
}
