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
package com.livelandr.flintcore.core.attachments;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AttachmentBase extends Item {
    protected String slot = "unique";
    public Set<String> requiredTags = new HashSet<>();

    public AttachmentBase(Properties pProperties) {
        super(pProperties);
    }

    public String getSlot() {
        return slot;
    }
    public void setSlot(String _slot) {
        this.slot = _slot;
    }
    public Set<String> getTags() {
        return requiredTags;
    }
    public void addTags(String tag) {
        requiredTags.add(tag);
    }

    public void onAttach(LivingEntity player, ItemStack attachment, ItemStack gun) {}
    public void onDetach(LivingEntity player, ItemStack attachment, ItemStack gun) {}


    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        if (Screen.hasAltDown()) {
            pTooltipComponents.add(Component.translatable("flintcore.guninfoattachmentslots").append(Component.translatable("flintcore.slotnames." + slot)));

            pTooltipComponents.add(Component.translatable("flintcore.guninfoattachmenttags"));
            for (String slot : this.requiredTags) {
                pTooltipComponents.add(Component.literal("   ").append(Component.translatable("flintcore.attachmenttag." + slot)));
            }
        }
    }
}
