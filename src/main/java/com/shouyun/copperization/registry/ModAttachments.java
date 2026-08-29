package com.shouyun.copperization.registry;

import com.shouyun.copperization.Copperization;
import com.shouyun.copperization.block.CopperizedChunkState;
import com.shouyun.copperization.copper.CopperizationState;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModAttachments {
	public static final AttachmentType<CopperizedChunkState> COPPERIZED_BLOCKS = AttachmentRegistry
		.<CopperizedChunkState>builder()
		.initializer(() -> CopperizedChunkState.EMPTY)
		.persistent(CopperizedChunkState.CODEC)
		.syncWith(ByteBufCodecs.fromCodecWithRegistries(CopperizedChunkState.CODEC), AttachmentSyncPredicate.all())
		.buildAndRegister(Copperization.id("copperized_blocks"));

	public static final AttachmentType<CopperizationState> COPPERIZATION_STATE = AttachmentRegistry
		.<CopperizationState>builder()
		.initializer(() -> CopperizationState.EMPTY)
		.persistent(CopperizationState.CODEC)
		.syncWith(ByteBufCodecs.fromCodecWithRegistries(CopperizationState.CODEC), AttachmentSyncPredicate.all())
		.buildAndRegister(Copperization.id("copperization_state"));

	private ModAttachments() {
	}

	public static void register() {
		Copperization.LOGGER.debug("Registered Copperization data attachments");
	}
}
