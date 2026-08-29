package com.shouyun.copperization.client.render;

import com.shouyun.copperization.block.CopperizableBlockRegistry;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;

/** Registers once; Fabric rebuilds wrappers safely on every model/resource reload. */
public final class CopperizedBlockModelPlugin {
	private CopperizedBlockModelPlugin() {
	}

	public static void register() {
		ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake()
			.register(ModelModifier.WRAP_LAST_PHASE, (model, modelContext) ->
				CopperizableBlockRegistry.supportsPositionCopperization(modelContext.state())
					? new CopperizedBlockStateModel(model) : model));
	}
}
