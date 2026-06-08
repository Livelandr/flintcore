package com.livelandr.flintcore.core;

import com.livelandr.flintcore.core.guns.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SiegeEngineAdapter {
    public LivingEntity scapegoatEnt = null;
    public ItemStack gun;
    public float rotationX, rotationY;

    public ItemStack getGun() {
        return gun;
    }

    public void setScapegoat(LivingEntity _scape) {
        scapegoatEnt = _scape;
    }

    public LivingEntity getScapegoat() {
        return scapegoatEnt;
    }

    public void setRotationX(float _val) {
        rotationX = _val;
    }
    public void setRotationY(float _val) {
        rotationY = _val;
    }

    public float getRotationX() {
        return rotationX;
    }
    public float getRotationY() {
        return rotationY;
    }

    public void setGun(ItemStack _gun) {
        gun = _gun;
    }

    public void createGun(GunBase _gunClass) {
        gun = new ItemStack(_gunClass);
    }

    public void transmitInteraction(Level pLevel, LivingEntity pPlayer, ItemStack gunStack, InteractionHand pUsedHand) {
        GunBase.getGunBase(gun).interaction(pLevel, pPlayer, gunStack, pUsedHand, true, rotationX, rotationY);
    }

    public void forceSetAmmo(ItemStack ammo) {
        GunBase gunItem = GunBase.getGunBase(gun);

        if (gunItem instanceof FlintlockBase) {
            CompoundTag ammoData = ammo.serializeNBT();
            gun.getTag().put("AmmoType", ammoData);
            gun.getTag().putBoolean("HasAmmo", true);
        } if (gunItem instanceof BlazelockBase || gunItem instanceof PumpActionBase) {
            gun.getTag().putInt("Ammo", 0);
            ((BlazelockBase) gunItem).AddAmmo(null, gun, ammo);
        } else if (gunItem instanceof MagfedBase MB) {
            MB.InsertMagazine(null, gun, ammo);
        }
    }

    public void forceShoot() {
        GunBase gunItem = GunBase.getGunBase(gun);
        gunItem.shoot(scapegoatEnt.level(), scapegoatEnt, gun, getRotationX(), getRotationY());
    }
}
