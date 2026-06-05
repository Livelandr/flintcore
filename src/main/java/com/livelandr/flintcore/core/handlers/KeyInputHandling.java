package com.livelandr.flintcore.core.handlers;

import com.livelandr.flintcore.Flintcore;
import com.livelandr.flintcore.core.network.PacketHandler;
import com.livelandr.flintcore.core.network.packets.C2S_ExtractMagazine;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Flintcore.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KeyInputHandling {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        if (KeyBindings.UNLOAD_MAGAZINE.consumeClick()) {
            PacketHandler.sendToServer(new C2S_ExtractMagazine());
        }
    }
}
