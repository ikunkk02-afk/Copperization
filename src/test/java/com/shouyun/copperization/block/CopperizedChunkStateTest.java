package com.shouyun.copperization.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class CopperizedChunkStateTest {
	@Test
	void storesSparseLocalPositionsAndRoundTripsCodec() {
		BlockPos first = new BlockPos(33, 72, -17);
		BlockPos second = new BlockPos(47, -32, -31);
		CopperizedChunkState state = CopperizedChunkState.EMPTY
			.put(first, CopperizedBlockData.fresh(1200L))
			.put(second, new CopperizedBlockData(3, true, 9999L));
		assertEquals(2, state.entries().size());
		assertEquals(3, state.get(second).oxidationStage());
		assertTrue(state.get(second).waxed());

		var encoded = CopperizedChunkState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
		CopperizedChunkState decoded = CopperizedChunkState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
		assertEquals(state, decoded);
		assertNull(decoded.remove(first).get(first));
	}
}
