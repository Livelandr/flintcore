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
package com.livelandr.flintcore.core.network.packets;

import com.livelandr.flintcore.core.guns.BlazelockBase;
import com.livelandr.flintcore.core.guns.MagfedBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2S_ExtractMagazine {

    public C2S_ExtractMagazine() {}

    public C2S_ExtractMagazine(FriendlyByteBuf buffer) {
        this();
    }

    public void encode(FriendlyByteBuf buffer) {}

    public void handler(Supplier<NetworkEvent.Context> context) {
        // TODO: PUT THIS CODE INTO ANOTHER HANDLER, INSTEAD OF PACKET DIRECTLY

        ServerPlayer player = context.get().getSender();
        if (player == null) {
            return;
        }

        ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (weapon.getItem() instanceof MagfedBase gun) {
            gun.__internal_ExtractMagazine(player, weapon);
        }
        if (weapon.getItem() instanceof BlazelockBase gun) {
            gun.openChamber(player, weapon);
        }
    }
}
