package com.livelandr.flintcore.core.handlers;

import com.livelandr.flintcore.Flintcore;
import com.livelandr.flintcore.core.guns.GunBase;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

@Mod.EventBusSubscriber(modid = Flintcore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEventBusClientEvents {

    static List<String> nbtpredicates = List.of(
            "IsCocked", "IsUncocked", "IsStuffed", "ChamberOpen", "ShootReady", "SlideCocked", "HaveMag"
    );

    static void addNewPredicateNBT(String NBT) {
        nbtpredicates.add(NBT);
    }

    static void createPredicatesForAllGunBases() {
        for (Item item : ForgeRegistries.ITEMS) {
            if (item instanceof GunBase) {


                for (String predicate : nbtpredicates) {
                    ItemProperties.register(
                            item,
                            fromNamespaceAndPath(Flintcore.MOD_ID, predicate.toLowerCase()),
                            (stack, level, entity, seed) -> {
                                return stack.getOrCreateTag().getBoolean(predicate) ? 1.0F : 0.0F;
                            }
                    );
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        createPredicatesForAllGunBases();
    }
}
