package com.shouyun.copperization.gametest;

import com.shouyun.copperization.copper.CopperizationManager;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

@SuppressWarnings("UnstableApiUsage")
public final class CopperizationClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			context.waitFor(client -> client.level != null && client.player != null);
			var connection = singleplayer.getConnection();
			singleplayer.getServer().runOnServer(server -> {
				var level = connection.getServerLevel();
				var player = connection.getServerPlayer();
				float[] stages = {0.25F, 0.50F, 0.75F, 1.0F};
				for (int index = 0; index < stages.length; index++) {
					var horse = EntityTypes.HORSE.create(level, EntitySpawnReason.COMMAND);
					if (horse == null) throw new AssertionError("Could not create render-test horse");
					horse.setPos(player.getX() + (index - 1.5D) * 2.2D, player.getY(), player.getZ() + 7.0D);
					horse.setYRot(180.0F);
					horse.setNoAi(true);
					if (!level.addFreshEntity(horse)) throw new AssertionError("Could not spawn render-test horse");
					CopperizationManager.setProgress(horse, stages[index]);
				}
			});
			connection.waitForClientboundEntityUpdates(EntityTypes.HORSE);
			connection.waitForChunksRender();
			context.waitTicks(20);
			context.takeScreenshot("copperization-render-stages");
		}
	}
}
