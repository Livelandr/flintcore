package com.livelandr.flintcore.core.ammo;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BaseGunpowder extends Item {
    public BaseGunpowder(Properties pProperties) {
        super(pProperties);
    }

    public void onGunpowderInserted(ItemStack gunpowder, LivingEntity ent, ItemStack gun) {
        gunpowder.shrink(1);
    }
}
