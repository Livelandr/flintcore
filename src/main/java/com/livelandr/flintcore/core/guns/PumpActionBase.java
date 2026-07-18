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

import com.livelandr.flintcore.core.util.HookContext;
import com.livelandr.flintcore.core.util.HookSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import com.livelandr.flintcore.core.ammo.BaseAmmo;

import javax.annotation.Nullable;
import java.util.List;

public class PumpActionBase extends GunBase {
    public PumpActionBase(Properties pProperties) {
        super(pProperties);
    }

    public boolean needCockToReload = true;
    public int maxAmmo = 6;

    public void OnCockStart(Level pLevel, LivingEntity shooter, ItemStack gun, InteractionHand pUsedHand) {
        
    }

    public void OnCockEnd(Level pLevel, LivingEntity shooter, ItemStack gun, InteractionHand pUsedHand) {
        
    }

    public int GetMaxAmmoAmount(ItemStack gun) {
        return ((PumpActionBase) gun.getItem()).maxAmmo;
    }

    public int GetAmmoAmount(ItemStack gun) {
        return gun.getTag().getInt("Ammo");
    }

    public void AddAmmo(LivingEntity shooter, ItemStack gun, ItemStack ammo) {
        BaseAmmo ammoType = (BaseAmmo) ammo.getItem();
        int totalInClip = ammoType.ammoCountInOne(ammo);

        ItemStack ammoStack = ammoType.getAmmoItemStack(ammo);

        for (int i = 0; i < ammoType.ammoCountInOne(ammo); i++) {
            int curAmmo = gun.getTag().getInt("Ammo");
            curAmmo++;

            if (curAmmo > GetMaxAmmoAmount(gun)) continue;
            totalInClip--;

            gun.getTag().putInt("Ammo", curAmmo);

            CompoundTag ammoData = ammoStack.serializeNBT();
            gun.getTag().put("AmmoType" + curAmmo, ammoData);
        }

        if (totalInClip > 0) {
            ammoStack.setCount(totalInClip);
            if (shooter instanceof Player ply) {
                if (!ply.getInventory().add(ammoStack)) {
                    ply.drop(ammoStack, false);
                }
            }
        }

        ammo.shrink(1);
    }

    public BaseAmmo GetFirstAmmo(ItemStack gun) {
        int curAmmo = gun.getTag().getInt("Ammo");
        ItemStack ammoData = ItemStack.of((CompoundTag) gun.getTag().get("AmmoType" + curAmmo));

        BaseAmmo ammo = (BaseAmmo) ammoData.getItem();

        curAmmo--;
        gun.getTag().putInt("Ammo", curAmmo);

        return ammo;
    }

    @Override
    public boolean tryShoot(Level pLevel, LivingEntity pPlayer, ItemStack gun, InteractionHand pUsedHand) {
        if (GetAmmoAmount(gun) == 0) return false;

        return true;
    }

    @Override
    public void shoot(Level pLevel, LivingEntity pPlayer, ItemStack gunStack, float rotationX, float rotationY) {
        BaseAmmo ammo = GetFirstAmmo(gunStack);

        if (HookSystem.calculateHookBool(new HookContext.Builder("processShooting")
                .shooter(pPlayer)
                .gun(gunStack)
                .rotationX(rotationX)
                .rotationY(rotationY)
                .ammoType(ammo)
                .build())) {

           ammo.onAmmoShot(rotationX, rotationY, pPlayer, gunStack, pLevel);
        }

        super.shoot(pLevel, pPlayer, gunStack, rotationX, rotationY);
    }

    @Override
    public boolean interaction(Level pLevel, LivingEntity pPlayer, ItemStack gunStack, InteractionHand pUsedHand, boolean proxy, float proxyX, float proxyY, LivingEntity proxyUser) {
        if (!checkCooldown(gunStack)) {
            return false;
        }
        ItemStack secondItemStack;

        if (!proxy) {
            if (pUsedHand == InteractionHand.MAIN_HAND)
                secondItemStack = pPlayer.getItemInHand(InteractionHand.OFF_HAND);
            else
                secondItemStack = pPlayer.getItemInHand(InteractionHand.MAIN_HAND);
        } else {
            secondItemStack = proxyUser.getItemInHand(pUsedHand);
        }


        if (!pLevel.isClientSide()) {
            if (!gunStack.hasTag()) gunStack.setTag(new CompoundTag());

            // Attachment
            if (checkAttachmentComparability(pPlayer, gunStack, secondItemStack.getItem())) {
                this.setAttachment(pPlayer, gunStack, secondItemStack);
                return true;
            }

            if (!needCockToReload && (checkAmmoCompatibility(secondItemStack.getItem())
                    || HookSystem.calculateHookBool(new HookContext.Builder(HookSystem.AMMO_COMPATIBILITY_OVERRIDE).gun(gunStack).shooter(pPlayer).ammoType(secondItemStack.getItem()).build()))
                && GetAmmoAmount(gunStack) < GetMaxAmmoAmount(gunStack)) {
                AddAmmo(pPlayer, gunStack, secondItemStack);
                onAmmo(pLevel, pPlayer, gunStack, secondItemStack, pUsedHand);
            } else if (!gunStack.getTag().getBoolean("IsUncocked")) {
                if (gunStack.getTag().getBoolean("ReadyToShoot")) {
                    if (allowPressingTrigger(pLevel, pPlayer, gunStack, pUsedHand) || (proxy && allowPressingTrigger(pLevel, proxyUser, gunStack, pUsedHand))) {
                        // Shoot
                        if (tryShoot(pLevel, pPlayer, gunStack, pUsedHand) || (proxy && tryShoot(pLevel, proxyUser, gunStack, pUsedHand))) {
                            if (!proxy) {
                                shoot(pLevel, pPlayer, gunStack);
                            } else {
                                shoot(pLevel, pPlayer, gunStack, proxyX, proxyY);
                            }
                        } else {
                            onTryFailure(pLevel, pPlayer, gunStack);
                        }
                        gunStack.getTag().putBoolean("ReadyToShoot", false);
                    }
                } else {
                    gunStack.getTag().putBoolean("IsUncocked", true);
                    OnCockStart(pLevel, pPlayer, gunStack, pUsedHand);
                }
            } else {
                if (needCockToReload &&
                (checkAmmoCompatibility(secondItemStack.getItem())
                || HookSystem.calculateHookBool(new HookContext.Builder(HookSystem.AMMO_COMPATIBILITY_OVERRIDE).gun(gunStack).shooter(pPlayer).ammoType(secondItemStack.getItem()).build()))
                && GetAmmoAmount(gunStack) < GetMaxAmmoAmount(gunStack)) {
                    AddAmmo(pPlayer, gunStack, secondItemStack);
                    onAmmo(pLevel, pPlayer, gunStack, secondItemStack, pUsedHand);
                } else {
                    gunStack.getTag().putBoolean("IsUncocked", false);
                    gunStack.getTag().putBoolean("ReadyToShoot", true);

                    OnCockEnd(pLevel, pPlayer, gunStack, pUsedHand);
                }
            }
        }

        return true;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);

        // Ammo + Chamber open
        if (pLevel != null && pStack.hasTag() ) {
            long time = pLevel.getGameTime();

            if (GetAmmoAmount(pStack) > 0) {
                pTooltipComponents.add(Component.translatable("flintcore.ammo").withStyle(ChatFormatting.GRAY).append(
                        Component.literal( String.valueOf(GetAmmoAmount(pStack)) ).append(
                                Component.literal("/").append(
                                        Component.literal(String.valueOf(GetMaxAmmoAmount(pStack)))))));

                // Output all loaded ammo
                for (int i = 0; i < GetAmmoAmount(pStack); i++) {
                    ItemStack ammoData = ItemStack.of((CompoundTag) pStack.getTag().get("AmmoType" + (i+1)));

                    pTooltipComponents.add(Component.literal(String.valueOf(i+1)).append(Component.literal(": ")).append(ammoData.getDisplayName()));
                }

            } else {
                ChatFormatting format;
                if (time % 10 < 5) {
                    format = ChatFormatting.GRAY;
                } else {
                    format = ChatFormatting.DARK_RED;
                }
                pTooltipComponents.add(Component.translatable("flintcore.no_payload").withStyle(format));
            }

            if (pStack.getTag().getBoolean("IsUncocked")) {
                ChatFormatting format;
                if (time % 10 < 5) {
                    format = ChatFormatting.GRAY;
                } else {
                    format = ChatFormatting.DARK_RED;
                }
                pTooltipComponents.add(Component.translatable("flintcore.uncocked").withStyle(format));
            }
        }
    }
}
