/*
 * Copyright (C) 2026 Livelandr
 *
 * This file is part of Flintcore.
 *
 * Flintcore is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Flintcore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.livelandr.flintcore.core.ammo;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BaseMagazine extends Item {

    // Guns requirements
    public Set<String> requiredMagazineTags = new HashSet<>();
    // Ammo possibilities
    public Set<String> allowedCalibersTags = new HashSet<>();

    public int maxAmmo = 30;

    public BaseMagazine(Properties pProperties) {
        super(pProperties.stacksTo(1));
    }

    public Set<String> getAllCaliberTags() {return allowedCalibersTags;}
    public boolean checkCaliberCompatibility(Set<String> requiredTags) {
        if (requiredTags.isEmpty()) return false;
        if (requiredTags.contains("universal")) return true;
        return getAllCaliberTags().containsAll(requiredTags);
    }

    public boolean allowAmmo(ItemStack ammo) {
        if (!(ammo.getItem() instanceof BaseAmmo)) return false;
        return checkAmmoCompatibility((BaseAmmo) ammo.getItem());
    }
    public boolean checkAmmoCompatibility(BaseAmmo ammo) {
        return checkCaliberCompatibility(ammo.requiredCaliberTags);
    }

    public void onAmmoInsert(ItemStack mag) {}
    public void onAmmoExtract(ItemStack mag) {}

    public void copyToGun(ItemStack mag, ItemStack gun) {
        gun.getTag().putInt("AmmoCount", BaseMagazine.getAmmo(mag));
        gun.getTag().putInt("MaxAmmoCount", ((BaseMagazine) mag.getItem()).getMaxAmmo(mag));

        for (int i = 0; i < BaseMagazine.getAmmo(mag); i++) {
            CompoundTag nbt = (CompoundTag) mag.getOrCreateTag().get("A"+String.valueOf(i));
            gun.getTag().put("A" + String.valueOf(i), nbt);
        }
    }

    public static void SetFromGun(ItemStack mag, ItemStack gun) {
        int count = gun.getTag().getInt("AmmoCount");
        mag.getTag().putInt("AmmoCount", count);

        for (int i = 0; i < count; i++) {
            CompoundTag nbt = (CompoundTag) gun.getTag().get("A"+String.valueOf(i));
            mag.getTag().put("A" + String.valueOf(i), nbt);
        }
    }

    // Not optimal, but well, it works
    public void addAmmo(ItemStack mag, ItemStack ammo) {
        int curAmmo = getAmmo(mag);

        CompoundTag ammoData = ammo.serializeNBT();

        mag.getTag().put("A" + String.valueOf(curAmmo), ammoData);
        mag.getTag().putInt("AmmoCount", curAmmo+1);

        ammo.shrink(1);
        onAmmoInsert(mag);
    }

    public static ItemStack getNAmmo(ItemStack mag, int n) {
        CompoundTag nbt = (CompoundTag) mag.getOrCreateTag().get("A"+String.valueOf(n));
        ItemStack stack = ItemStack.of( nbt );

        return stack;
    }

    public static ItemStack getLastAmmo(ItemStack mag) {
        return getNAmmo(mag,getAmmo(mag)-1);
    }

    public ItemStack extractLastAmmo(ItemStack mag) {
        int curAmmo = getAmmo(mag);
        ItemStack ammo = getLastAmmo(mag);
        ammo.setCount(1);
        mag.getTag().putInt("AmmoCount", curAmmo-1);
        onAmmoExtract(mag);
        return ammo;
    }

    public static int getAmmo(ItemStack mag) {
        return mag.getOrCreateTag().getInt("AmmoCount");
    }

    public int getMaxAmmo(ItemStack mag) {
        return this.maxAmmo;
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        // Getting hand and offhand item
        ItemStack magStack = pPlayer.getItemInHand(pUsedHand);

        ItemStack secondItemStack;
        if (pUsedHand == InteractionHand.MAIN_HAND)
            secondItemStack = pPlayer.getItemInHand(InteractionHand.OFF_HAND);
        else
            secondItemStack = pPlayer.getItemInHand(InteractionHand.MAIN_HAND);

        if (!pPlayer.isCrouching()) {

            if (allowAmmo(secondItemStack) && getAmmo(magStack) < getMaxAmmo(magStack)) {
                addAmmo(magStack, secondItemStack);
                pPlayer.getCooldowns().addCooldown(this, 2);
                if (pLevel.isClientSide()) {
                    pLevel.playLocalSound(pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
                            SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 1.0F, 1.0F, false);
                }
            }
        } else if (getAmmo(magStack) > 0) {
            pPlayer.getCooldowns().addCooldown(this, 2);
            if (pLevel.isClientSide()) {
                pLevel.playLocalSound(pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 1.0F, 1.0F, false);
            } else {
                ItemStack item = extractLastAmmo(magStack);
                if (!pPlayer.getInventory().add(item)) {
                    pPlayer.drop(item, false);
                }
            }
        }

        return InteractionResultHolder.pass(pPlayer.getItemInHand(pUsedHand));
    }


    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {

        // Statuses
        if (pLevel != null) {
            if (getAmmo(pStack) > 0) {
                pTooltipComponents.add(Component.translatable("flintcore.ammo").append(
                        String.valueOf(getAmmo(pStack))).append("/").append(String.valueOf(getMaxAmmo(pStack))).withStyle(ChatFormatting.GRAY));
                pTooltipComponents.add(Component.literal(""));

                pTooltipComponents.add(Component.translatable("flintcore.magazine"));

                for (int i = 0; i < getAmmo(pStack); i++) {
                    ItemStack item = BaseMagazine.getNAmmo(pStack, i);
                    pTooltipComponents.add(Component.literal(String.valueOf(i+1)).append(": ").append(item.getDisplayName()));
                }
            } else {
                pTooltipComponents.add(Component.translatable("flintcore.magempty").withStyle(ChatFormatting.RED));
                pTooltipComponents.add(Component.translatable("flintcore.ammo").append(
                        String.valueOf(getAmmo(pStack))).append("/").append(String.valueOf(getMaxAmmo(pStack))).withStyle(ChatFormatting.GRAY));
            }
            pTooltipComponents.add(Component.literal(""));
        }

        if (!Screen.hasShiftDown()) {
            pTooltipComponents.add(Component.translatable("flintcore.guninfoshift"));
            pTooltipComponents.add(Component.literal(""));
        } else {
            pTooltipComponents.add(Component.translatable("flintcore.guninfoammo"));
            for (String ammo : allowedCalibersTags) {
                pTooltipComponents.add(Component.literal("   ").append(Component.translatable("flintcore.calibernames." + ammo)));
            }

            pTooltipComponents.add(Component.literal(""));
        }

        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }
}
