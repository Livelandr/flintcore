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

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2C_RecoilCameraOffsetPacket {
    private float Xrot;
    private float Yrot;

    public S2C_RecoilCameraOffsetPacket(float X, float Y) {
        this.Xrot = X;
        this.Yrot = Y;
    }

    public S2C_RecoilCameraOffsetPacket(FriendlyByteBuf buffer) {
        this(buffer.readFloat(), buffer.readFloat());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(Xrot);
        buffer.writeFloat(Yrot);
    }

    public void handler(Supplier<NetworkEvent.Context> context) {
        LocalPlayer player = Minecraft.getInstance().player;

        player.setXRot(player.getXRot()+Xrot);
        player.setYRot(player.getYRot()+Yrot);
    }
}
