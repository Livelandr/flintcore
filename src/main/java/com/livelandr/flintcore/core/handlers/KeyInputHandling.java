package com.livelandr.flintcore.core.handlers;

import com.livelandr.flintcore.Flintcore;
import com.livelandr.flintcore.core.guns.GunBase;
import com.livelandr.flintcore.core.network.PacketHandler;
import com.livelandr.flintcore.core.network.packets.C2S_ExtractMagazine;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
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


    // Stinky hack to avoid slow down because of Use function
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player ply = event.getEntity();

        if (ply.isUsingItem()) {
            ItemStack stack = ply.getUseItem();

            if (stack.getItem() instanceof GunBase) {
                event.getInput().leftImpulse /= 0.2F;
                event.getInput().forwardImpulse /= 0.2F;
            }
        }
    }
}
