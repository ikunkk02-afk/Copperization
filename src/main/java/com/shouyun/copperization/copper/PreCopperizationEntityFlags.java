package com.shouyun.copperization.copper;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** State changed only while an entity is held as a copper statue. */
public record PreCopperizationEntityFlags(boolean noAi, boolean invulnerable, boolean noGravity) {
	public static final PreCopperizationEntityFlags DEFAULT = new PreCopperizationEntityFlags(false, false, false);
	public static final Codec<PreCopperizationEntityFlags> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.BOOL.optionalFieldOf("no_ai", false).forGetter(PreCopperizationEntityFlags::noAi),
		Codec.BOOL.optionalFieldOf("invulnerable", false).forGetter(PreCopperizationEntityFlags::invulnerable),
		Codec.BOOL.optionalFieldOf("no_gravity", false).forGetter(PreCopperizationEntityFlags::noGravity)
	).apply(instance, PreCopperizationEntityFlags::new));

	public static PreCopperizationEntityFlags capture(LivingEntity entity) {
		return new PreCopperizationEntityFlags(entity instanceof Mob mob && mob.isNoAi(), entity.isInvulnerable(), entity.isNoGravity());
	}

	public void restore(LivingEntity entity) {
		entity.setInvulnerable(invulnerable);
		entity.setNoGravity(noGravity);
		if (entity instanceof Mob mob) mob.setNoAi(noAi);
	}
}
