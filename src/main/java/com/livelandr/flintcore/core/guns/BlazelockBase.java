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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import com.livelandr.flintcore.core.ammo.BaseAmmo;

import javax.annotation.Nullable;
import java.util.List;

public class BlazelockBase extends GunBase {
    public BlazelockBase(Properties pProperties) {
        super(pProperties);
    }

    public int maxAmmo = 2;
    public boolean needCocking = false;

    public void onCocking(Level pLevel, LivingEntity shooter, ItemStack gun) {
        setCooldown(shooter, gun, shootCooldown(shooter, gun));
    }

    public void onChamberOpen(Level pLevel, LivingEntity shooter, ItemStack gun) {
        gun.getTag().putBoolean("IsCocked", false);
        setReloadAnimation(gun);
        
        setCooldown(shooter, gun, cooldownTicks);
    }

    public void onChamberClose(Level pLevel, LivingEntity shooter, ItemStack gun) {
        setAimAnimation(gun);
        
        setCooldown(shooter, gun, cooldownTicks);
    }

    public void onAmmoInsert(Level pLevel, LivingEntity shooter, ItemStack gun, InteractionHand pUsedHand) {
        pLevel.playSound(null, shooter,
                SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 1.0F, 1);

        
        setCooldown(shooter, gun, shootCooldown(shooter, gun));
    }

    public int GetMaxAmmoAmount(ItemStack gun) {
        return ((BlazelockBase) gun.getItem()).maxAmmo;
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
    public void __internal_shoot(Level pLevel, LivingEntity pPlayer, ItemStack gunStack, float rotationX, float rotationY) {
        BaseAmmo ammo = GetFirstAmmo(gunStack);

        gunStack.getTag().putBoolean("IsCocked", false);
        if (HookSystem.calculateHookBool(new HookContext.Builder("processShooting")
                .shooter(pPlayer)
                .gun(gunStack)
                .rotationX(rotationX)
                .rotationY(rotationY)
                .ammoType(ammo)
                .build(), 1)) {
            ammo.onAmmoShot(rotationX, rotationY, pPlayer, gunStack, pLevel);
        }

        if (GetAmmoAmount(gunStack) == 0) gunStack.getTag().putBoolean("ShootReady", false);
        super.__internal_shoot(pLevel, pPlayer, gunStack, rotationX, rotationY);
    }

    @Override
    public boolean tryShoot(Level pLevel, LivingEntity pPlayer, ItemStack gun, InteractionHand pUsedHand) {
        if (GetAmmoAmount(gun) == 0) return false;

        return true;
    }

    public void openChamber(LivingEntity ply, ItemStack gun) {
        gun.getTag().putBoolean("ChamberOpen", true);
        onChamberOpen(ply.level(), ply, gun);
    }

    // TODO: Fix all this noodle code, it's so f ugly I can't
    @Override
    public void __internal_interaction(Level pLevel, LivingEntity pPlayer, ItemStack gunStack, InteractionHand pUsedHand, boolean proxy, float proxyX, float proxyY, LivingEntity proxyUser) {
        if (!__internal_checkCooldown(gunStack)) {
            return;
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

        if (!gunStack.hasTag()) gunStack.setTag(new CompoundTag());

        // Attachment
        if (checkAttachmentComparability(pPlayer, gunStack, secondItemStack.getItem())) {
            this.setAttachment(pPlayer, gunStack, secondItemStack);
            return;
        }

        // If everything is done - shoot
        if (gunStack.getTag().getBoolean("ShootReady") && !gunStack.getTag().getBoolean("ChamberOpen")) {
            if (allowPressingTrigger(pLevel, pPlayer, gunStack, pUsedHand) || (proxy && allowPressingTrigger(pLevel, proxyUser, gunStack, pUsedHand))) {
                if (!needCocking || gunStack.getTag().getBoolean("IsCocked")) {
                    if (tryShoot(pLevel, pPlayer, gunStack, pUsedHand) || (proxy && tryShoot(pLevel, proxyUser, gunStack, pUsedHand))) {
                        if (!proxy) {
                            __internal_shoot(pLevel, pPlayer, gunStack);
                        } else {
                            __internal_shoot(pLevel, pPlayer, gunStack, proxyX, proxyY);
                        }
                    } else {
                        onTryFailure(pLevel, pPlayer, gunStack);
                        gunStack.getTag().putBoolean("ShootReady", false);
                    }
                } else {
                    gunStack.getTag().putBoolean("IsCocked", true);
                    onCocking(pLevel, pPlayer, gunStack);
                }
            }
        } else {
            // If chamber opened - try to insert ammo, or close
            if (gunStack.getTag().getBoolean("ChamberOpen")) {
                // If ammo is less than max
                if (GetAmmoAmount(gunStack) < GetMaxAmmoAmount(gunStack)) {
                if (checkAmmoCompatibility(secondItemStack.getItem())
                        || HookSystem.calculateHookBool(new HookContext.Builder(HookSystem.AMMO_COMPATIBILITY_OVERRIDE).gun(gunStack).shooter(pPlayer).ammoType(secondItemStack.getItem()).build(), 0)) {
                        AddAmmo(pPlayer, gunStack, secondItemStack);
                        onAmmoInsert(pLevel, pPlayer, gunStack, pUsedHand);
                        ((BaseAmmo) secondItemStack.getItem()).onAmmoInsert(pPlayer, gunStack, secondItemStack);
                    } else {
                        gunStack.getTag().putBoolean("ChamberOpen", false);
                        onChamberClose(pLevel, pPlayer, gunStack);
                        gunStack.getTag().putBoolean("ShootReady", true);
                    }
                } else {
                    gunStack.getTag().putBoolean("ChamberOpen", false);
                    onChamberClose(pLevel, pPlayer, gunStack);
                    gunStack.getTag().putBoolean("ShootReady", true);
                }

            } else {
                openChamber(pPlayer, gunStack);
            }
        }


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
                if (Screen.hasControlDown()) {
                    for (int i = 0; i < GetAmmoAmount(pStack); i++) {
                        ItemStack ammoData = ItemStack.of((CompoundTag) pStack.getTag().get("AmmoType" + (i + 1)));

                        pTooltipComponents.add(Component.literal(String.valueOf(i + 1)).append(Component.literal(": ")).append(ammoData.getDisplayName()));
                    }
                } else {
                    pTooltipComponents.add(Component.translatable("flintcore.guninfoctrl"));
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

            if (pStack.getTag().getBoolean("ChamberOpen")) {
                ChatFormatting format;
                if (time % 10 < 5) {
                    format = ChatFormatting.GRAY;
                } else {
                    format = ChatFormatting.DARK_RED;
                }
                pTooltipComponents.add(Component.translatable("flintcore.chamberopen").withStyle(format));
            }

            if (this.needCocking && pStack.getTag().getBoolean("ShootReady")) {
                if (pStack.getTag().getBoolean("IsCocked")) {
                    pTooltipComponents.add(Component.translatable("flintcore.cocked").withStyle(ChatFormatting.GREEN));
                } else {
                    ChatFormatting format;
                    if (time % 10 < 5) {
                        format = ChatFormatting.DARK_RED;
                    } else {
                        format = ChatFormatting.GRAY;
                    }
                    pTooltipComponents.add(Component.translatable("flintcore.uncocked").withStyle(format));
                }
            }
        }
    }
}
