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
package com.livelandr.flintcore.core.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import com.livelandr.flintcore.core.network.PacketHandler;
import com.livelandr.flintcore.core.network.packets.S2C_RecoilCameraOffsetPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class CameraWork {
    public static void OffsetEntityCamera(Entity player, float Xrot, float Yrot) {
        if (!(player instanceof Player)) return;
        if (!player.level().isClientSide()) {
            PacketHandler.sendToPlayer(new S2C_RecoilCameraOffsetPacket(Xrot, Yrot), (ServerPlayer) player);
        }
    }
    // Future VR Mod compatibility investment
    public static float getPlayerViewX(LivingEntity ply) {
        return ply.getXRot();
    }
    public static float getPlayerViewY(LivingEntity ply) {
        return ply.getYRot();
    }
}
