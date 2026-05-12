package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.entity.vehicle.MinecartHopper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
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
        // handle dest container
        Direction Facing = ((HopperBlockEntityAccessor) BE).getFacing();
        BlockPos ToPos = Pos.relative(Facing);
        Container Dest = HopperBlockEntity.getContainerAt(W, ToPos);
        if (Dest instanceof Entity) {
            // do nothing if the hopper is locked and dest is an entity (container minecarts, eg)
            if (((LockableBlockEntity) BE).Nguhcraft$GetLock() != null)
                CIR.setReturnValue(false);
        } else {
            // continue only if dest container has the same lock (or both are null)
            LockableBlockEntity ToBE = (LockableBlockEntity) Dest;
            if (ToBE != null && ((LockableBlockEntity) BE).Nguhcraft$GetLock() != ToBE.Nguhcraft$GetLock())
                CIR.setReturnValue(false);
        }
        
        // handle source container
        BlockPos FromPos = Pos.above();
        Container Source = HopperBlockEntity.getContainerAt(W, FromPos);
        if (Source instanceof Entity) {
            // do nothing if the hopper is locked and dest is an entity (container minecarts, eg)
            if (((LockableBlockEntity) BE).Nguhcraft$GetLock() != null)
                CIR.setReturnValue(false);
        } else {
            // continue only if source container has the same lock (or both are null)
            LockableBlockEntity FromBE = (LockableBlockEntity) Source;
            if (FromBE != null && ((LockableBlockEntity) BE).Nguhcraft$GetLock() != FromBE.Nguhcraft$GetLock())
                CIR.setReturnValue(false);
        }
    }
}
