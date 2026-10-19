package com.velorise.veinfarming.mixin;

import com.velorise.veinfarming.FarmingConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmBlock.class)
public abstract class FarmBlockMixin {

    @Inject(method = "turnToDirt", at = @At("HEAD"), cancellable = true)
    private static void vein_farming$preventTrample(Entity entity, BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        if (entity != null && FarmingConfig.PREVENT_FARMLAND_TRAMPLE.get()) {
            ci.cancel();
        }
    }
}

