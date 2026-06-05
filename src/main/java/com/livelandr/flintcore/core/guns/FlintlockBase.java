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
package com.livelandr.flintcore.core.guns;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;
import com.livelandr.flintcore.core.ammo.BaseAmmo;

import javax.annotation.Nullable;
import java.util.List;

public class FlintlockBase extends GunBase {
    public FlintlockBase(Properties pProperties) {
        super(pProperties);
    }

    public boolean noCock = false;
    public int GunpowderRequired = 1;
    public int ramrodCooldownTicks = 20;
    public int gunpowderCooldownTicks = 20;

    public int gunpowderCooldown(Player ply, ItemStack gunStack) {
        return gunpowderCooldownTicks;
    }
    public int ramrodCooldown(Player ply, ItemStack gunStack) {
        return ramrodCooldownTicks;
    }

    public void onGunpowder(Level pLevel, LivingEntity shooter, ItemStack gun, InteractionHand pUsedHand) {
        pLevel.playSeededSound(null, shooter.getBlockX(), shooter.getBlockY(), shooter.getBlockZ(),
                SoundEvents.SAND_BREAK, SoundSource.NEUTRAL, 1F, 1.0F, 0);

        if (shooter instanceof Player ply) {
            ply.getCooldowns().addCooldown(this, gunpowderCooldown(ply, gun));
        }
    }

    public void onStuff(Level pLevel, LivingEntity shooter, ItemStack gun, InteractionHand pUsedHand) {
        setAimAnimation(gun);

        if (shooter instanceof Player ply) {
            ply.getCooldowns().addCooldown(this, ramrodCooldown(ply, gun));
        }
    }

    public void onCock(Level pLevel, LivingEntity shooter, ItemStack gun, InteractionHand pUsedHand) {
        setAimAnimation(gun);

        if (shooter instanceof Player ply) {
            ply.getCooldowns().addCooldown(this, shootCooldown(ply, gun));
        }
    }

    @Override
    public void shoot(Level pLevel, LivingEntity pPlayer, ItemStack gunStack) {
        super.shoot(pLevel, pPlayer, gunStack);

        ItemStack ammoData = ItemStack.of((CompoundTag) gunStack.getTag().get("AmmoType"));

        BaseAmmo ammo = (BaseAmmo) ammoData.getItem();
        ammo.onAmmoShot(pPlayer, gunStack, pLevel);

        gunStack.getTag().putInt("Gunpowder", 0);
        gunStack.getTag().putBoolean("HasAmmo", false);
        gunStack.getTag().putBoolean("IsCocked", false);
        gunStack.getTag().putBoolean("IsStuffed", false);

        setReloadAnimation(gunStack);
    }

    public boolean isRamrod(ItemStack item) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        // Getting hand and offhand item
        ItemStack gunStack = pPlayer.getItemInHand(pUsedHand);

        ItemStack secondItemStack;
        if (pUsedHand == InteractionHand.MAIN_HAND)
            secondItemStack = pPlayer.getItemInHand(InteractionHand.OFF_HAND);
        else
            secondItemStack = pPlayer.getItemInHand(InteractionHand.MAIN_HAND);

        if (!pLevel.isClientSide()) {
            if (!gunStack.hasTag()) gunStack.setTag(new CompoundTag());

            // Attachment
            if (checkAttachmentComparability(pPlayer, gunStack, secondItemStack.getItem())) {
                this.setAttachment(pPlayer, gunStack, secondItemStack);
                return InteractionResultHolder.success(pPlayer.getItemInHand(pUsedHand));
            }

            // If everything is done - shoot
            if (gunStack.getTag().getBoolean("IsCocked") || (noCock && gunStack.getTag().getBoolean("IsStuffed"))) {
                if (allowPressingTrigger(pLevel, pPlayer, gunStack, pUsedHand)) {
                    if (tryShoot(pLevel, pPlayer, gunStack, pUsedHand)) {
                        shoot(pLevel, pPlayer, gunStack);
                        onShoot(pLevel, pPlayer, gunStack);
                    } else {
                        onTryFailure(pLevel, pPlayer, gunStack);
                    }
                }
            }

        // Try to add gunpowder if isn't added
        if (gunStack.getTag().getInt("Gunpowder") < GunpowderRequired) {
            // Add gunpowder
            if (secondItemStack.is(Tags.Items.GUNPOWDER)) {
                gunStack.getTag().putInt("Gunpowder", gunStack.getTag().getInt("Gunpowder")+1);
                secondItemStack.shrink(1);
                onGunpowder(pLevel, pPlayer, gunStack, pUsedHand);
            }
        } else {
            // Try to add ammo
            if (!gunStack.getTag().getBoolean("HasAmmo")) {
                if (checkAmmo(secondItemStack.getItem())) {
                    // Putting Ammo
                    CompoundTag ammoData = secondItemStack.serializeNBT();
                    gunStack.getTag().put("AmmoType", ammoData);
                    gunStack.getTag().putBoolean("HasAmmo", true);

                    secondItemStack.shrink(1);
                    onAmmo(pLevel, pPlayer, secondItemStack, gunStack, pUsedHand);
                }
            } else {
                if (!gunStack.getTag().getBoolean("IsStuffed")) {
                    if (isRamrod(secondItemStack)) {
                        gunStack.getTag().putBoolean("IsStuffed", true);
                        onStuff(pLevel, pPlayer, gunStack, pUsedHand);
                    }
                } else {
                    // Try to cock
                    if (!gunStack.getTag().getBoolean("IsCocked") && !noCock) {
                        gunStack.getTag().putBoolean("IsCocked", true);

                        onCock(pLevel, pPlayer, gunStack, pUsedHand);
                    }
                }
            }

        }
        }

        return InteractionResultHolder.pass(pPlayer.getItemInHand(pUsedHand));
    }


    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);

        // Statuses
        if (pLevel != null) {
            if (pStack.getOrCreateTag().getInt("Gunpowder") < GunpowderRequired) {
                pTooltipComponents.add(Component.translatable("flintcore.gunpowder").append(
                        String.valueOf(pStack.getTag().getInt("Gunpowder"))).append("/").append(String.valueOf(GunpowderRequired)).withStyle(ChatFormatting.RED));
            } else {
                pTooltipComponents.add(Component.translatable("flintcore.gunpowder").append(
                        String.valueOf(pStack.getTag().getInt("Gunpowder"))).append("/").append(String.valueOf(GunpowderRequired)).withStyle(ChatFormatting.DARK_GREEN));
                if (!pStack.getTag().getBoolean("HasAmmo")) {
                    pTooltipComponents.add(Component.translatable("flintcore.no_payload").withStyle(ChatFormatting.RED));
                } else {
                    ItemStack ammoData = ItemStack.of((CompoundTag) pStack.getTag().get("AmmoType"));

                    pTooltipComponents.add(Component.translatable("flintcore.payload").append(ammoData.getDisplayName()).withStyle(ChatFormatting.DARK_GREEN));
                    if (!pStack.getTag().getBoolean("IsStuffed")) {
                        pTooltipComponents.add(Component.translatable("flintcore.not_stuffed").withStyle(ChatFormatting.RED));
                    } else {
                        pTooltipComponents.add(Component.translatable("flintcore.ready_to_shoot").withStyle(ChatFormatting.DARK_GREEN));
                    }
                }
            }


        }
    }
}
