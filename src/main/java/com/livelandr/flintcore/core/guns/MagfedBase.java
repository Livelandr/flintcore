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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import com.livelandr.flintcore.core.ammo.BaseAmmo;
import com.livelandr.flintcore.core.ammo.BaseMagazine;

import javax.annotation.Nullable;
import java.util.List;

public class MagfedBase extends GunBase {
    public MagfedBase(Properties pProperties) {
        super(pProperties);
    }

    public boolean needSlideAfterShot = false;

    public boolean checkMagCompatibility(BaseMagazine mag) {
        return checkCaliberCompatibility(mag.requiredMagazineTags);
    }

    public boolean checkMagazine(ItemStack mag) {
        if (!(mag.getItem() instanceof BaseMagazine)) return false;

        return checkMagCompatibility((BaseMagazine) mag.getItem());
    }

    // Events
    public void onSlideStart(Level pLevel, LivingEntity shooter, ItemStack gun) {
        setCooldown(shooter, gun, 10);
    }

    public void onSlideEnd(Level pLevel, LivingEntity shooter, ItemStack gun) {

        setCooldown(shooter, gun, 15);
    }

    public void onMagExtract(Level pLevel, LivingEntity shooter, ItemStack gun) {
        setReloadAnimation(gun);

        setCooldown(shooter, gun, 15);
    }

    public void onMagInsert(Level pLevel, LivingEntity shooter, ItemStack gun) {
        setAimAnimation(gun);
        setCooldown(shooter, gun, 15);
    }

    // Events end

    public void InsertMagazine(LivingEntity ply, ItemStack gun, ItemStack mag) {
        if (gun.getTag().getBoolean("HaveMag")) return;

        ((BaseMagazine) mag.getItem()).copyToGun(mag, gun);

        CompoundTag magData = mag.serializeNBT();
        gun.getTag().put("Magazine", magData);
        gun.getTag().putBoolean("HaveMag", true);

        mag.shrink(1);

        onMagInsert(ply.level(), ply, gun);
    }

    public void ExtractMagazine(LivingEntity ent, ItemStack gun) {
        if (!gun.getTag().getBoolean("HaveMag")) return;

        CompoundTag nbt = (CompoundTag) gun.getTag().get("Magazine");
        ItemStack magazineStack = ItemStack.of(nbt);

        gun.getTag().putBoolean("ShootReady", false);
        gun.getTag().putBoolean("HaveMag", false);

        BaseMagazine.SetFromGun(magazineStack, gun);

        if (ent instanceof Player ply) {
            if (!ply.getInventory().add(magazineStack)) {
                ply.drop(magazineStack, false);
            }
        }
        // Not neccessary, but just for safety
        gun.getTag().putInt("AmmoCount", 0);
        gun.getTag().putInt("MaxAmmoCount", 0);

        onMagExtract(ent.level(), ent, gun);
    }

    public int GetAmmoAmount(ItemStack gun) {
        return gun.getOrCreateTag().getInt("AmmoCount");
    }

    public static ItemStack getNAmmo(ItemStack gun, int n) {
        CompoundTag nbt = (CompoundTag) gun.getOrCreateTag().get("A"+String.valueOf(n));
        ItemStack stack = ItemStack.of( nbt );

        return stack;
    }

    public ItemStack GetFirstAmmoStack(ItemStack gun) {
        int curAmmo = gun.getTag().getInt("AmmoCount");
        ItemStack ammoData = ItemStack.of((CompoundTag) gun.getTag().get("A" + (curAmmo-1)));

        return ammoData;
    }

    @Nullable
    public BaseAmmo GetFirstAmmo(ItemStack gun) {
        int curAmmo = gun.getTag().getInt("AmmoCount");
        ItemStack ammoData = ItemStack.of((CompoundTag) gun.getTag().get("A" + (curAmmo-1)));

        if (!(ammoData.getItem() instanceof BaseAmmo)) {
            return null;
        }

        BaseAmmo ammo = (BaseAmmo) ammoData.getItem();

        curAmmo--;
        gun.getTag().putInt("AmmoCount", curAmmo);

        return ammo;
    }

    // Chamber stuff
    public ItemStack getChamberStack(ItemStack gun) {
        ItemStack stack = ItemStack.of((CompoundTag) gun.getTag().get("Chamber"));
        return stack;
    }

    public ItemStack ejectChamberStack(ItemStack gun) {
        ItemStack stack = getChamberStack(gun);
        zeroChamber(gun);

        return stack;
    }

    public boolean chamberLoaded(ItemStack gun) {
        ItemStack stack = getChamberStack(gun);
        return (stack.getItem() != Items.AIR);
    }

    public void zeroChamber(ItemStack gun) {
        CompoundTag empty = (new ItemStack(Items.AIR)).serializeNBT();
        gun.getTag().put("Chamber", empty);
    }

    public void putInChamber(ItemStack gun, ItemStack ammo) {
        CompoundTag ammoData = ammo.serializeNBT();
        gun.getTag().put("Chamber", ammoData);
    }

    public void loadToChamber(ItemStack gun) {
        BaseAmmo ammo = GetFirstAmmo(gun);
        if (ammo != null) {
            putInChamber(gun, new ItemStack(ammo));
        }
    }

    // Chamber stuff end

    public int GetMaxAmmoAmount(ItemStack pStack) {
        return pStack.getTag().getInt("MaxAmmoCount");
    }

    @Override
    public void shoot(Level pLevel, LivingEntity pPlayer, ItemStack gunStack, float rotationX, float rotationY) {
        super.shoot(pLevel, pPlayer, gunStack, rotationX, rotationY);
        BaseAmmo ammo = (BaseAmmo) ejectChamberStack(gunStack).getItem();

        ammo.onAmmoShot(rotationX, rotationY, pPlayer, gunStack, pLevel);
        if (chamberLoaded(gunStack)) gunStack.getTag().putBoolean("ShootReady", false);
    }

    @Override
    public boolean tryShoot(Level pLevel, LivingEntity pPlayer, ItemStack gun, InteractionHand pUsedHand) {
        if (!chamberLoaded(gun)) return false;

        return true;
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

        if (!gunStack.hasTag()) gunStack.setTag(new CompoundTag());

        // Attachment
        if (checkAttachmentComparability(pPlayer, gunStack, secondItemStack.getItem())) {
            this.setAttachment(pPlayer, gunStack, secondItemStack);
            return true;
        }
        // I'm sleep-deprived hi
        if (gunStack.getTag().getBoolean("ShootReady") || (chamberLoaded(gunStack) && !checkMagazine(secondItemStack))) {
            if (gunStack.getTag().getBoolean("HaveMag") || chamberLoaded(gunStack)) {
                if (allowPressingTrigger(pLevel, pPlayer, gunStack, pUsedHand) || (proxy && allowPressingTrigger(pLevel, proxyUser, gunStack, pUsedHand))) {
                    if (tryShoot(pLevel, pPlayer, gunStack, pUsedHand) || (proxy && tryShoot(pLevel, proxyUser, gunStack, pUsedHand))) {
                        if (!proxy) {
                            shoot(pLevel, pPlayer, gunStack);
                        } else {
                            shoot(pLevel, pPlayer, gunStack, proxyX, proxyY);
                        }

                        if (needSlideAfterShot) {
                            gunStack.getTag().putBoolean("SlideCocked", false);
                            gunStack.getTag().putBoolean("ShootReady", false);
                        } else {
                            loadToChamber(gunStack);
                        }
                    } else {
                        onTryFailure(pLevel, pPlayer, gunStack);
                        gunStack.getTag().putBoolean("ShootReady", false);
                    }
                }
            } else {
                onTryFailure(pLevel, pPlayer, gunStack);
                gunStack.getTag().putBoolean("ShootReady", false);
            }
        } else {
            if (!gunStack.getTag().getBoolean("HaveMag")) {
                if (checkMagazine(secondItemStack)) {
                    InsertMagazine(pPlayer, gunStack, secondItemStack);
                }
            } else if (GetAmmoAmount(gunStack) <= 0) {
                ExtractMagazine(pPlayer, gunStack);
            } else if (!gunStack.getTag().getBoolean("SlideCocked")) {
                gunStack.getTag().putBoolean("SlideCocked", true);
                onSlideStart(pLevel, pPlayer, gunStack);
            } else {
                gunStack.getTag().putBoolean("SlideCocked", false);
                gunStack.getTag().putBoolean("ShootReady", true);
                onSlideEnd(pLevel, pPlayer, gunStack);

                // Chamber
                loadToChamber(gunStack);
            }
        }


        return true;
    }



    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        //super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
        pTooltipComponents.add(Component.literal(""));
        if (showTier) {
            if (this.getWeaponTier() == -1) {
                pTooltipComponents.add(Component.translatable("flintcore.weapontieruniversal"));
            } else {
                pTooltipComponents.add(Component.translatable("flintcore.weapontier").append(Integer.toString(this.getWeaponTier())));
            }
        }

        int totalAttach = 0;
        for (String type : attachmentSlots) {
            if (isAttachmentValidAndEnabled(pStack, type)) {
                ItemStack item = getAttachmentStack(pStack, type);
                pTooltipComponents.add(Component.translatable("flintcore.attachment").append(item.getDisplayName()));
                item.getItem().appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);

                totalAttach++;
            }
        }
        if (totalAttach > 0) {
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

            if (!attachmentSlots.isEmpty()) {
                pTooltipComponents.add(Component.translatable("flintcore.guninfoattachmentslots"));
                for (String slot : attachmentSlots) {
                    pTooltipComponents.add(Component.literal("   ").append(Component.translatable("flintcore.slotnames." + slot)));
                }
                pTooltipComponents.add(Component.translatable("flintcore.guninfoattachmenttags"));
                for (String slot : this.allowedAttachmentsTags) {
                    pTooltipComponents.add(Component.literal("   ").append(Component.translatable("flintcore.attachmenttag." + slot)));
                }
            } else {
                pTooltipComponents.add(Component.translatable("flintcore.guninfonoattachment"));
            }

            pTooltipComponents.add(Component.literal(""));
        }

        // Chamber + Magazine info
        if (pLevel != null && pStack.hasTag() ) {
            long time = pLevel.getGameTime();
            ChatFormatting format;
            if (time % 10 < 5) {
                format = ChatFormatting.GRAY;
            } else {
                format = ChatFormatting.DARK_RED;
            }

            if (this.chamberLoaded(pStack)) {
                pTooltipComponents.add(Component.translatable("flintcore.liveround").withStyle(format));
            } else {
                pTooltipComponents.add(Component.translatable("flintcore.chamberempty"));
            }

            if (!pStack.getTag().getBoolean("HaveMag")) {
                pTooltipComponents.add(Component.translatable("flintcore.no_magazine").withStyle(format));
            } else {
                CompoundTag nbt = (CompoundTag) pStack.getTag().get("Magazine");
                ItemStack magazineStack = ItemStack.of(nbt);
                pTooltipComponents.add(Component.translatable("flintcore.magazine").withStyle(ChatFormatting.GRAY).
                        append(magazineStack.getDisplayName()));

                if (GetAmmoAmount(pStack) > 0) {
                    pTooltipComponents.add(Component.translatable("flintcore.ammo").withStyle(ChatFormatting.GRAY).append(
                            Component.literal(String.valueOf(GetAmmoAmount(pStack))).append(
                                    Component.literal("/").append(
                                            Component.literal(String.valueOf(GetMaxAmmoAmount(pStack)))))));

                    // Output all loaded ammo
                    if (Screen.hasControlDown()) {
                        for (int i = 0; i < GetAmmoAmount(pStack); i++) {
                            ItemStack ammoData = ItemStack.of((CompoundTag) pStack.getTag().get("A" + i));

                            pTooltipComponents.add(Component.literal(String.valueOf(i + 1)).append(Component.literal(": ")).append(ammoData.getDisplayName()));
                        }
                    } else {
                        pTooltipComponents.add(Component.translatable("flintcore.guninfoctrl"));
                    }
                } else {
                    pTooltipComponents.add(Component.translatable("flintcore.no_payload").withStyle(format));
                }
            }

        }
    }
}
