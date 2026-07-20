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

import com.livelandr.flintcore.core.util.CameraWork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BaseAmmo extends Item {

    public boolean showTier = false;
    public int tier = 0;
    @ApiStatus.Internal
    public Set<String> requiredCaliberTags = new HashSet<>();

    public float damage = 0;
    public boolean customDescription = false;

    public BaseAmmo(Properties pProperties) {
        super(pProperties);
    }

    public void addRequiredTag(String tag) {
        this.requiredCaliberTags.add(tag);
    }

    public void onAmmoShot(float xRotation, float yRotation, LivingEntity shooter, ItemStack gun, Level level) {}
    public void onAmmoShot(LivingEntity shooter, ItemStack gun, Level level)  {
        onAmmoShot(CameraWork.getPlayerViewX(shooter), CameraWork.getPlayerViewY(shooter), shooter, gun, level);
    }

    public void onAmmoInsert(LivingEntity shooter, ItemStack gun, ItemStack ammo) {
        ammo.shrink(1);
    }

    public int ammoCountInOne(ItemStack ammo) {
        return 1;
    }
    public ItemStack getAmmoItemStack(ItemStack ammo) {
        return ammo;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        if (!customDescription) {
            pTooltipComponents.add(Component.literal(""));
            if (showTier) pTooltipComponents.add(Component.translatable("flintcore.weapontier").append(Integer.toString(tier)));
            pTooltipComponents.add(Component.translatable("flintcore.ammoinfotags"));
            for (String ammo : this.requiredCaliberTags) {
                pTooltipComponents.add(Component.literal("   ").append(Component.translatable("flintcore.calibernames." + ammo)));
            }

            pTooltipComponents.add(Component.literal(""));
            pTooltipComponents.add(Component.translatable("flintcore.bullet_description"));
            pTooltipComponents.add(Component.translatable("flintcore.projectile_damage")
                    .append(String.valueOf(Math.round(this.damage))).withStyle(ChatFormatting.DARK_GREEN));
        }
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }
}
