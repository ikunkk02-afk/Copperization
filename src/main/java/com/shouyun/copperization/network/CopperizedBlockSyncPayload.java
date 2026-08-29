package com.shouyun.copperization.network;

import com.shouyun.copperization.Copperization;
import com.shouyun.copperization.block.CopperizedBlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.Nullable;

/** Small server-to-client delta for a live chunk attachment change. */
public record CopperizedBlockSyncPayload(BlockPos pos, @Nullable CopperizedBlockData data) implements CustomPacketPayload {
	public static final Type<CopperizedBlockSyncPayload> TYPE = new Type<>(Copperization.id("copperized_block_sync"));
	public static final StreamCodec<FriendlyByteBuf, CopperizedBlockSyncPayload> STREAM_CODEC = CustomPacketPayload.codec(
		(payload, buffer) -> {
			buffer.writeBlockPos(payload.pos);
			buffer.writeBoolean(payload.data != null);
			if (payload.data != null) {
				buffer.writeVarInt(payload.data.oxidationStage());
				buffer.writeBoolean(payload.data.waxed());
				buffer.writeLong(payload.data.nextOxidationTick());
			}
		},
		buffer -> {
			BlockPos pos = buffer.readBlockPos();
			if (!buffer.readBoolean()) return new CopperizedBlockSyncPayload(pos, null);
			return new CopperizedBlockSyncPayload(pos, new CopperizedBlockData(buffer.readVarInt(), buffer.readBoolean(), buffer.readLong()));
		}
	);

	@Override
	public Type<CopperizedBlockSyncPayload> type() {
		return TYPE;
	}
}
