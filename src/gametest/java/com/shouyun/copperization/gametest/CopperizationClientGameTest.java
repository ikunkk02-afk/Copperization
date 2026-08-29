package com.shouyun.copperization.gametest;

import com.shouyun.copperization.copper.CopperizationManager;
import com.shouyun.copperization.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

@SuppressWarnings("UnstableApiUsage")
public final class CopperizationClientGameTest implements FabricClientGameTest {
	private static final float[] OXIDATION_PROGRESS = {0.0F, 1.0F / 3.0F, 2.0F / 3.0F, 1.0F};
	private static final String[] STAGE_NAMES = {"fresh", "exposed", "weathered", "oxidized"};

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			context.waitFor(client -> client.level != null && client.player != null);
			TestServerConnection connection = singleplayer.getConnection();
			singleplayer.getServer().runCommand("time set noon");
			singleplayer.getServer().runCommand("weather clear");

			captureEntityStages(context, singleplayer, connection, EntityTypes.ZOMBIE, "zombie");
			captureEntityStages(context, singleplayer, connection, EntityTypes.SKELETON, "skeleton");
			captureEntityStages(context, singleplayer, connection, EntityTypes.VILLAGER, "villager");
			captureEntityStages(context, singleplayer, connection, EntityTypes.HORSE, "horse");
			captureEntityStages(context, singleplayer, connection, EntityTypes.CREEPER, "creeper");
			captureWaxParity(context, singleplayer, connection);
			captureBlockPanels(context, singleplayer, connection);
		}
	}

	private static <T extends LivingEntity> void captureEntityStages(
		ClientGameTestContext context,
		TestSingleplayerContext singleplayer,
		TestServerConnection connection,
		EntityType<T> type,
		String name
	) {
		List<LivingEntity> entities = singleplayer.getServer().computeOnServer(server -> {
			ServerLevel level = connection.getServerLevel();
			var player = connection.getServerPlayer();
			List<LivingEntity> spawned = new ArrayList<>();
			for (int stage = 0; stage < OXIDATION_PROGRESS.length; stage++) {
				T entity = type.create(level, EntitySpawnReason.COMMAND);
				if (entity == null) throw new AssertionError("Could not create render-test " + name);
				// The test camera mirrors world X, so reverse placement to keep screenshots
				// readable from left to right: Fresh, Exposed, Weathered, Oxidized.
				entity.setPos(player.getX() + (1.5D - stage) * 2.2D, player.getY(), player.getZ() + 7.0D);
				entity.setYRot(180.0F);
				if (entity instanceof Mob mob) mob.setNoAi(true);
				if (!level.addFreshEntity(entity)) throw new AssertionError("Could not spawn render-test " + name);
				CopperizationManager.setProgress(entity, 1.0F);
				CopperizationManager.setState(entity, CopperizationManager.getState(entity)
					.withOxidation(OXIDATION_PROGRESS[stage], level.getGameTime() + 1200L));
				spawned.add(entity);
			}
			return spawned;
		});
		connection.waitForClientboundEntityUpdates(type);
		connection.waitForChunksRender();
		context.waitTicks(10);
		context.takeScreenshot("copperization-statue-stages-" + name);
		singleplayer.getServer().runOnServer(server -> entities.forEach(LivingEntity::discard));
		connection.waitForClientboundEntityUpdates(type);
		context.waitTicks(3);
	}

	private static void captureWaxParity(
		ClientGameTestContext context,
		TestSingleplayerContext singleplayer,
		TestServerConnection connection
	) {
		for (int stage = 0; stage < OXIDATION_PROGRESS.length; stage++) {
			int stageIndex = stage;
			List<LivingEntity> entities = singleplayer.getServer().computeOnServer(server -> {
				ServerLevel level = connection.getServerLevel();
				var player = connection.getServerPlayer();
				List<LivingEntity> spawned = new ArrayList<>();
				for (int waxed = 0; waxed < 2; waxed++) {
					var creeper = EntityTypes.CREEPER.create(level, EntitySpawnReason.COMMAND);
					if (creeper == null) throw new AssertionError("Could not create wax parity creeper");
					creeper.setPos(player.getX() + (waxed == 0 ? -1.2D : 1.2D), player.getY(), player.getZ() + 7.0D);
					creeper.setYRot(180.0F);
					creeper.setNoAi(true);
					if (!level.addFreshEntity(creeper)) throw new AssertionError("Could not spawn wax parity creeper");
					CopperizationManager.setProgress(creeper, 1.0F);
					CopperizationManager.setState(creeper, CopperizationManager.getState(creeper)
						.withOxidation(OXIDATION_PROGRESS[stageIndex], level.getGameTime() + 1200L)
						.withWaxed(waxed == 1));
					spawned.add(creeper);
				}
				return spawned;
			});
			connection.waitForClientboundEntityUpdates(EntityTypes.CREEPER);
			context.waitTicks(6);
			context.takeScreenshot("copperization-wax-parity-" + STAGE_NAMES[stage]);
			singleplayer.getServer().runOnServer(server -> entities.forEach(LivingEntity::discard));
			connection.waitForClientboundEntityUpdates(EntityTypes.CREEPER);
			context.waitTicks(3);
		}
	}

	private static void captureBlockPanels(
		ClientGameTestContext context,
		TestSingleplayerContext singleplayer,
		TestServerConnection connection
	) {
		int familyCount = ModBlocks.families().size();
		for (int panelStart = 0; panelStart < familyCount; panelStart += 4) {
			int firstFamily = panelStart;
			int lastFamily = Math.min(panelStart + 4, familyCount);
			List<BlockPos> positions = singleplayer.getServer().computeOnServer(server -> {
				ServerLevel level = connection.getServerLevel();
				BlockPos playerPos = connection.getServerPlayer().blockPosition();
				List<BlockPos> placed = new ArrayList<>();
				for (int familyIndex = firstFamily; familyIndex < lastFamily; familyIndex++) {
					var weathering = ModBlocks.families().get(familyIndex).blocks().weathering();
					List<Block> stages = List.of(weathering.unaffected(), weathering.exposed(), weathering.weathered(), weathering.oxidized());
					int row = familyIndex - firstFamily;
					for (int stage = 0; stage < stages.size(); stage++) {
						BlockPos pos = new BlockPos(playerPos.getX() + 1 - stage, playerPos.getY() - 1 + row, playerPos.getZ() + 7);
						level.setBlockAndUpdate(pos, stages.get(stage).defaultBlockState());
						placed.add(pos);
					}
				}
				return placed;
			});
			context.waitTick();
			connection.waitForClientboundPackets();
			connection.waitForChunksRender(false);
			context.waitTicks(5);
			context.takeScreenshot("copperization-block-stages-panel-" + (panelStart / 4 + 1));
			singleplayer.getServer().runOnServer(server -> {
				ServerLevel level = connection.getServerLevel();
				positions.forEach(pos -> level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()));
			});
			context.waitTick();
			connection.waitForClientboundPackets();
		}
	}
}
