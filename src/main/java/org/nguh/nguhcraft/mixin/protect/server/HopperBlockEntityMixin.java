package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.nguh.nguhcraft.item.LockableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    /**
    * Prevent hoppers & hopper minecarts from taking items from locked inventories.
    * <p>
    * canTakeItemFromContainer is called to check if a hopper (minecart) can take an item from a container.
    * Overwriting the return value to false if the hopper isnt permitted to take items.
    */
    @ModifyReturnValue(
        method = "canTakeItemFromContainer",
        at = @At("RETURN")
    )
    private static boolean inject$mayExtract(boolean Original, @Local(ordinal = 0) Container Hopper, @Local(ordinal = 1) Container Source) {
        // return false if it failed original check
        if (!Original)
            return false;

        // if the hopper is a minecart hopper
        if (!(Hopper instanceof LockableBlockEntity)) {
            // if the source container is a minecart container
            if (!(Source instanceof LockableBlockEntity))
                return true;
            // check for lock if the source container is a block entity
            return ((LockableBlockEntity) Source).Nguhcraft$GetLock() == null;
        }
        
        // if the source is a minecart container
        if (!(Source instanceof LockableBlockEntity))
            return ((LockableBlockEntity) Hopper).Nguhcraft$GetLock() == null;

        // check if locks match
        return ((LockableBlockEntity) Hopper).Nguhcraft$GetLock() == ((LockableBlockEntity) Source).Nguhcraft$GetLock();
    }

    /**
    * Prevent hoppers from putting items into locked inventories.
    * <p>
    * This code sets the Container variable used in ejectItems as destination container to null,
    * if the hopper is not permitted to store any items there.
    */
    @ModifyVariable(
        method = "ejectItems",
        at = @At(value = "STORE")
    )
    private static Container inject$insert(Container Dest, @Local HopperBlockEntity Hopper) {
        LockableBlockEntity LBE = (LockableBlockEntity)Hopper;
    
        // if dest isnt a lockableblockentity (container minecarts, eg) then always refuse if locked
        if (!(Dest instanceof LockableBlockEntity) && LBE.Nguhcraft$GetLock() != null)
            return null;

        // if dest is a lockableblockentity, then compare locks and disallow if they dont match (both unlocked counts as matching)
        if (Dest instanceof LockableBlockEntity && LBE.Nguhcraft$GetLock() != ((LockableBlockEntity)Dest).Nguhcraft$GetLock())
            return null;

        // if all lock checks succeed, then just return the original value
        return Dest;
    }
}