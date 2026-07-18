package com.livelandr.flintcore.core.util;

import com.livelandr.flintcore.core.ammo.BaseAmmo;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class HookContext {
    private final String id;

    private LivingEntity shooter;
    private ItemStack gun;
    private float rotationX;
    private float rotationY;
    private Item ammoType;

    private HookContext(Builder builder) {
        this.id = builder.id;
        this.shooter = builder.shooter;
        this.gun = builder.gun;
        this.rotationX = builder.rotationX;
        this.rotationY = builder.rotationY;
        this.ammoType = builder.ammoType;
    }

    // Getters
    public String getContextId() {
        return this.id;
    }
    public LivingEntity getShooter() {
        return shooter;
    }

    public ItemStack getGun() {
        return gun;
    }

    public float getRotationX() {
        return rotationX;
    }

    public float getRotationY() {
        return rotationY;
    }

    public Item getAmmoType() {
        return ammoType;
    }


    // Builder
    public static class Builder {
        private String id;

        private LivingEntity shooter;
        private ItemStack gun;
        private float rotationX;
        private float rotationY;
        private Item ammoType;

        public Builder(String systemId) {
            this.id = systemId;
        }

        public Builder shooter(LivingEntity shooter) {
            this.shooter = shooter;
            return this;
        }

        public Builder gun(ItemStack gun) {
            this.gun = gun;
            return this;
        }

        public Builder rotationX(float rotationX) {
            this.rotationX = rotationX;
            return this;
        }

        public Builder rotationY(float rotationY) {
            this.rotationY = rotationY;
            return this;
        }

        public Builder ammoType(Item ammoType) {
            this.ammoType = ammoType;
            return this;
        }

        public HookContext build() {
            return new HookContext(this);
        }
    }
}
