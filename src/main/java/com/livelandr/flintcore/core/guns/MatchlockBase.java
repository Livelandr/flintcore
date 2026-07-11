package com.livelandr.flintcore.core.guns;

import com.livelandr.flintcore.core.ammo.BaseGunpowder;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class MatchlockBase extends FlintlockBase {
    public MatchlockBase(Properties pProperties) {
        super(pProperties);
    }

    public void onIgnition(Level level, LivingEntity ply, ItemStack gun) {
        setCooldown(ply, gun, 5);
        level.playSound(null, ply, SoundEvents.TNT_PRIMED, SoundSource.NEUTRAL, 1, 1);
    }

    public boolean isIgniter(ItemStack item) {
        return item.is(Items.FLINT_AND_STEEL);
    }

    @Override
    public void shoot(Level pLevel, LivingEntity pPlayer, ItemStack gunStack, float floatX, float floatY) {
        super.shoot(pLevel, pPlayer, gunStack, floatX, floatY);

        gunStack.getTag().putBoolean("IsIgnited", false);
        setCooldown(pPlayer, gunStack, ramrodCooldown(pPlayer, gunStack));
    }

    @Override
    public boolean interaction(Level pLevel, LivingEntity pPlayer, ItemStack gunStack, InteractionHand pUsedHand, boolean proxy, float proxyX, float proxyY, LivingEntity proxyUser) {
        if (!checkCooldown(gunStack)) {
            return false;
        }

        ItemStack secondItemStack;


        if (!proxy) {
            if (pUsedHand == InteractionHand.MAIN_HAND)
                secondItemStack = pPlayer.getItemInHand(InteractionHand.OFF_HAND);
            else
                secondItemStack = pPlayer.getItemInHand(InteractionHand.MAIN_HAND);
        } else {
            secondItemStack = proxyUser.getItemInHand(pUsedHand);
        }

        if (!gunStack.hasTag()) gunStack.setTag(new CompoundTag());

        // Attachment
        if (checkAttachmentComparability(pPlayer, gunStack, secondItemStack.getItem())) {
            this.setAttachment(pPlayer, gunStack, secondItemStack);
            return true;
        }

        // If everything is done - shoot
        if (gunStack.getTag().getBoolean("HasAmmo") && gunStack.getTag().getBoolean("IsIgnited") && gunStack.getTag().getInt("Gunpowder") >= gunpowderRequired && (gunStack.getTag().getBoolean("IsCocked") || (noCock && gunStack.getTag().getBoolean("IsStuffed")) || (noStuff && noCock))) {
            if (allowPressingTrigger(pLevel, pPlayer, gunStack, pUsedHand) || (proxy && allowPressingTrigger(pLevel, proxyUser, gunStack, pUsedHand))) {
                if (tryShoot(pLevel, pPlayer, gunStack, pUsedHand) || (proxy && tryShoot(pLevel, proxyUser, gunStack, pUsedHand))) {
                    if (!proxy) {
                        shoot(pLevel, pPlayer, gunStack);
                    } else {
                        shoot(pLevel, pPlayer, gunStack, proxyX, proxyY);
                    }
                } else {
                    onTryFailure(pLevel, pPlayer, gunStack);
                }
            }
        }

        // Try to add gunpowder if isn't added
        if (gunStack.getTag().getInt("Gunpowder") < gunpowderRequired) {
            // Add gunpowder
            if (isGunpowder(secondItemStack)) {
                gunStack.getTag().putInt("Gunpowder", gunStack.getTag().getInt("Gunpowder")+1);
                onGunpowder(pLevel, pPlayer, gunStack, pUsedHand);

                if (secondItemStack.getItem() instanceof BaseGunpowder) {
                    ((BaseGunpowder) secondItemStack.getItem()).onGunpowderInserted(secondItemStack, pPlayer, gunStack);
                } else {
                    secondItemStack.shrink(1);
                }
            }
        } else {
            // Try to add ammo
            if (!gunStack.getTag().getBoolean("HasAmmo")) {
                if (checkAmmoCompatibility(secondItemStack.getItem())) {
                    // Putting Ammo
                    CompoundTag ammoData = secondItemStack.serializeNBT();
                    gunStack.getTag().put("AmmoType", ammoData);
                    gunStack.getTag().putBoolean("HasAmmo", true);

                    secondItemStack.shrink(1);
                    onAmmo(pLevel, pPlayer, gunStack, secondItemStack, pUsedHand);
                }
            } else {
                if (!gunStack.getTag().getBoolean("IsStuffed") && !noStuff) {
                    if (isRamrod(secondItemStack)) {
                        gunStack.getTag().putBoolean("IsStuffed", true);
                        onStuff(pLevel, pPlayer, gunStack, pUsedHand);
                    }
                } else if (!gunStack.getTag().getBoolean("IsIgnited")){
                    if (isIgniter(secondItemStack)) {
                        gunStack.getTag().putBoolean("IsIgnited", true);
                        onIgnition(pLevel, pPlayer, gunStack);
                    }
                } else if (!gunStack.getTag().getBoolean("IsCocked") && !noCock) {
                    // Try to cock
                    gunStack.getTag().putBoolean("IsCocked", true);
                    onCock(pLevel, pPlayer, gunStack, pUsedHand);
                }
            }


        }

        return true;
    }


    @Override
    public void onInventoryTick(ItemStack stack, Level level, Player player, int slotIndex, int selectedIndex) {
        if (player.isUnderWater() && stack.getTag().getBoolean("IsIgnited") ) {
            stack.getTag().putBoolean("IsIgnited", false);
            level.playSound(null, player, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 1, 1);
        }
        super.onInventoryTick(stack, level, player, slotIndex, selectedIndex);
    }


    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);

        // Statuses
        if (!pStack.getTag().getBoolean("IsIgnited")) {
            pTooltipComponents.add(Component.translatable("flintcore.not_ignited").withStyle(ChatFormatting.RED));
        } else {
            pTooltipComponents.add(Component.translatable("flintcore.ready_to_shoot").withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
