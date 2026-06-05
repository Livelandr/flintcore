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
@Mod(Flintcore.MOD_ID)
public class Flintcore
{

    public static final String MOD_ID = "flintcore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Flintcore(FMLJavaModLoadingContext context)
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

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("Flintcore initialized on server.");
    }
}
