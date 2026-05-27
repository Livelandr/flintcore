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
package com.livelandr.flintcore.core.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import com.livelandr.flintcore.flintcore;
import com.livelandr.flintcore.core.network.packets.S2C_RecoilCameraOffsetPacket;


public class PacketHandler {
    private static int packetId = 0;
    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel INSTANCE = NetworkRegistry.ChannelBuilder.named(
            new ResourceLocation(flintcore.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions((version) -> true)
            .serverAcceptedVersions((version) -> true)
            .simpleChannel();

    public static void register() {
        INSTANCE.messageBuilder(S2C_RecoilCameraOffsetPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2C_RecoilCameraOffsetPacket::encode)
                .decoder(S2C_RecoilCameraOffsetPacket::new)
                .consumerMainThread(S2C_RecoilCameraOffsetPacket::handler)
                .add();
    }

    public static void sendToServer(Object msg) {
        INSTANCE.send(PacketDistributor.SERVER.noArg(), msg);
    }

    public static void sendToPlayer(Object msg, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }
}
