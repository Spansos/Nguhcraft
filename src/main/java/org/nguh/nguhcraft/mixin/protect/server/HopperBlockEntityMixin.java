package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.entity.vehicle.MinecartHopper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.item.KeyItem;
import org.nguh.nguhcraft.item.LockableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    /** As below. Used also by hopper minecarts. */
    @Inject(
        method = "suckInItems(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/Hopper;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void inject$extract(Level W, Hopper H, CallbackInfoReturnable<Boolean> CIR) {
        BlockPos Pos = BlockPos.containing(H.getLevelX(), H.getLevelY() + 1.0, H.getLevelZ());
        LockableBlockEntity LBE = KeyItem.GetLockableEntity(W, Pos);
        // minecart hopper can never interact with any locked containers.
        // only the minecart case needs to be handled here, normal hoppers only call this function from insertAndExtract
        if (H instanceof MinecartHopper && LBE != null && LBE.Nguhcraft$GetLock() != null)
            CIR.setReturnValue(false);
    }

    /**
    * Prevent hoppers from accessing protected inventories.
    * <p>
    * Thanks to whatever FUCKING MORON designed the part of the fabric
    * API that overrides the hopper code TO STILL PERFORM THE TRANSFER
    * inside of the functions THAT RETRIEVE THE INVENTORIES to transfer
    * from/to, we need to perform this check early, instead of doing the
    * SENSIBLE thing and simply returning null for the input and output
    * inventories.
    */
    @Inject(
        method = "tryMoveItems",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void inject$insertAndExtract(
        Level W,
        BlockPos Pos,
        BlockState St,
        HopperBlockEntity BE,
        BooleanSupplier BS,
        CallbackInfoReturnable<Boolean> CIR
    ) {
        // I could not care less about being granular here and allowing inserting
        // and not extracting after having to deal with fabric’s asinine API design;
        // if a hopper is touching a protected block, then fuck you, nothing is going
        // to move anywhere.
        Direction Facing = ((HopperBlockEntityAccessor) BE).getFacing();
        
        // continue only if insert container has the same lock (or both are null)
        BlockPos ToPos = Pos.relative(Facing);
        LockableBlockEntity ToBE = KeyItem.GetLockableEntity(W, ToPos);
        if (ToBE != null && ((LockableBlockEntity) BE).Nguhcraft$GetLock() != ToBE.Nguhcraft$GetLock())
            CIR.setReturnValue(false);
        
        // continue only if extract container has the same lock (or both are null)
        BlockPos FromPos = Pos.above();
        LockableBlockEntity FromBE = KeyItem.GetLockableEntity(W, FromPos);
        if (FromBE != null && ((LockableBlockEntity) BE).Nguhcraft$GetLock() != FromBE.Nguhcraft$GetLock())
            CIR.setReturnValue(false);
    }
}
