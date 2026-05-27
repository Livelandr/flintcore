package com.livelandr.flintcore;

import com.livelandr.flintcore.core.network.PacketHandler;
import com.livelandr.flintcore.core.util.ServerTickHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(flintcore.MOD_ID)
public class flintcore
{

    public static final String MOD_ID = "flintcore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public flintcore(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(ServerTickHandler.INSTANCE);

        PacketHandler.register();
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("Flintcore initializing common...");
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("Flintcore initialized on server.");
    }
}
