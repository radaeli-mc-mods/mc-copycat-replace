package com.radaeli.copycatreplace.client;

import com.radaeli.copycatreplace.CopycatReplace;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.simibubi.create.content.decoration.copycat.CopycatModel;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Supplies the virtual Copycat's material instead of the real world's model-data cache. */
@EventBusSubscriber(modid = CopycatReplace.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class PonderCopycatModel extends BakedModelWrapper<BakedModel> {
    private PonderCopycatModel(BakedModel original) { super(original); }

    @SubscribeEvent
    public static void wrap(ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((location, model) ->
                model instanceof CopycatModel ? new PonderCopycatModel(model) : model);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
        if (level instanceof PonderLevel ponder && ponder.scene != null
                && CopycatReplace.MOD_ID.equals(ponder.scene.getNamespace())
                && level.getBlockEntity(pos) instanceof CopycatBlockEntity copycat) {
            data = copycat.getModelData();
        }
        return originalModel.getModelData(level, pos, state, data);
    }
}
