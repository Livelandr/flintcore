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
package com.livelandr.flintcore.core.guns;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.livelandr.flintcore.Flintcore;
import com.livelandr.flintcore.core.util.CameraWork;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.util.Lazy;
import com.livelandr.flintcore.core.FlintcoreHook;
import com.livelandr.flintcore.core.ammo.BaseAmmo;
import com.livelandr.flintcore.core.attachments.AttachmentBase;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class GunBase extends Item {

    protected final Lazy<Multimap<Attribute, AttributeModifier>> lazyAttributeMap;

    // Tier-Tag system stuff
    public boolean showTier = false;
    public int weaponTier = -1;
    public Set<String> allowedCalibersTags = new HashSet<>();
    public Set<String> allowedAttachmentsTags = new HashSet<>();
    public Set<String> attachmentSlots = new HashSet<>();

    public int cooldownTicks = 20;
    public int shootCooldownTicks = 20;
    public int ammoCooldownTicks = 20;

    // HOOKS SYSTEM
    // TODO: REPLACE HOOKS TO OTHER STATIC CLASS
    public static Map<String, List<FlintcoreHook>> hooks = new HashMap<>();
    static {
        hooks.put("calculateDamageModifier", new ArrayList<>());
        hooks.put("calculateRecoilModifierX", new ArrayList<>());
        hooks.put("calculateRecoilModifierY", new ArrayList<>());
        hooks.put("calculatePropellantModifier", new ArrayList<>());
        hooks.put("calculateAccuracyModifier", new ArrayList<>());

        hooks.put("tryShoot", new ArrayList<>());
        hooks.put("onShoot", new ArrayList<>());
    }
    public static float calculateHookSum(String hookName, LivingEntity shooter, ItemStack gun, float baseValue) {
        List<FlintcoreHook> funcs = hooks.get(hookName);
        if (funcs == null || funcs.isEmpty()) {
            return 1;
        }

        float baseVal = baseValue;

        for (FlintcoreHook hook : funcs) {
            baseVal *= hook.process(shooter, gun, baseVal);
        }

        return baseVal;
    };
    public static void triggerHooks(String hookName, LivingEntity shooter, ItemStack gun) {
        List<FlintcoreHook> funcs = hooks.get(hookName);
        if (funcs == null || funcs.isEmpty()) {
            return;
        }

        for (FlintcoreHook hook : funcs) {
            hook.process(shooter, gun, 0);
        }
    }
    // HOOKS SYSTEM END

    // Static thing, just prettier than casting, sadly there is no inline in java (?) :(
    public static GunBase getGunBase(ItemStack gun) {
        return (GunBase) gun.getItem();
    }

    public GunBase(Properties pProperties) {
        super(pProperties);

        // INITIALIZING HOOKS
        this.lazyAttributeMap = Lazy.of(() -> {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.put(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            BASE_ATTACK_DAMAGE_UUID,
                            "Weapon modifier",
                            2,
                            AttributeModifier.Operation.ADDITION
                    ));
            builder.put(Attributes.ATTACK_SPEED,
                    new AttributeModifier(
                            BASE_ATTACK_SPEED_UUID,
                            "Weapon modifier",
                            -2.4,
                            AttributeModifier.Operation.ADDITION
                    ));

            return builder.build();
        });
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (slotChanged) return true;
        return false;
    }

    // Tier-Tag
    public int getWeaponTier() {
        return weaponTier;
    }

    // -1 Weapon tier = inf
    public boolean checkTier(int requiredTier) {
        if (getWeaponTier() == -1) return true;
        return (getWeaponTier() >= requiredTier);
    }
    // Caliber tags getter
    public Set<String> getAllCaliberTags() {return allowedCalibersTags;}
    // Attachment tags getter
    public Set<String> getAllAttachmentsTags() {return allowedAttachmentsTags;}
    // Adding compatible calibers tag
    public void addCompatibleCaliberTag(String caliber) {
        allowedCalibersTags.add(caliber);
    }
    // Adding compatible attachments tag
    public void addCompatibleAttachmentTag(String tag) {
        allowedAttachmentsTags.add(tag);
    }
    // Adding compatible attachment slot
    public void addAttachmentSlot(String slot) {
        attachmentSlots.add(slot);
    }
    // Check if attachment slot present
    public boolean haveAttachmentSlot(String slot) {
        return attachmentSlots.contains(slot);
    }
    // Check if attachment tag present
    public boolean checkCaliberCompatibility(Set<String> requiredTags) {
        if (requiredTags.isEmpty()) return false;
        if (requiredTags.contains("universal")) return true;
        return getAllCaliberTags().containsAll(requiredTags);
    }
    // Check ammunition compatibility
    public boolean checkAmmoCompatibility(BaseAmmo ammo) {
        if (!checkTier(ammo.tier)) return false;
        return checkCaliberCompatibility(ammo.requiredCaliberTags);
    }
    // Check ammunition compability with Item directly
    public boolean checkAmmoCompatibility(Item ammo) {
        if (!(ammo instanceof BaseAmmo)) return false;
        return checkAmmoCompatibility((BaseAmmo) ammo);
    }
    // Check if attachment compatilble (slot + all tags)
    public boolean checkAttachmentComparability(LivingEntity ply, ItemStack gun, Item attachment) {
        if (!(attachment instanceof AttachmentBase)) return false;
        AttachmentBase atch = (AttachmentBase) attachment;

        if (!haveAttachmentSlot(atch.getSlot())) return false;
        return getAllAttachmentsTags().containsAll(atch.getTags());
    }
    // Install attachment to slot
    public void setAttachment(LivingEntity ply, ItemStack gun, ItemStack attachment) {
        CompoundTag attachmentData = gun.getTag().getCompound("Attachments");
        String attachType = ((AttachmentBase) attachment.getItem()).getSlot();
        // Return old attachment
        if (isAttachmentValidAndEnabled(gun, attachType)) {
            detachAttachment(ply, gun, attachType);
        }
        CompoundTag newAttachments = new CompoundTag();
        newAttachments.putBoolean("enabled", true);

        CompoundTag attachItem = attachment.serializeNBT();
        newAttachments.put("item", attachItem);
        attachmentData.put(attachType, newAttachments);

        ((AttachmentBase) attachment.getItem()).onAttach(ply, attachment, gun);

        gun.getTag().put("Attachments", attachmentData);
    }
    // Remove attachment from slot
    public void detachAttachment(LivingEntity ent, ItemStack gun, String type) {
        ItemStack detached = getAttachmentStack(gun, type);
        ((AttachmentBase) detached.getItem()).onDetach(ent, detached, gun);
        if (ent instanceof Player ply) {
            if (!ply.getInventory().add(detached)) {
                ply.drop(detached, false);
            }
        }
        gun.getOrCreateTag().getCompound("Attachments").getCompound(type).putBoolean("enabled", false);
    }
    // Get ItemStack from attachment
    public ItemStack getAttachmentStack(ItemStack gun, String type) {
        CompoundTag attachmentsData = gun.getOrCreateTag().getCompound("Attachments");

        if (!attachmentsData.getCompound(type).getBoolean("enabled")) {
            return ItemStack.EMPTY;
        }

        CompoundTag item = attachmentsData.getCompound(type).getCompound("item");
        ItemStack deserializedAttachment = ItemStack.of( item );
        deserializedAttachment.deserializeNBT(attachmentsData.getCompound(type).getCompound("item"));

        return deserializedAttachment;
    }
    // If there is some attachment in slot
    public boolean isAttachmentValidAndEnabled(ItemStack gun, String slot) {
        return (getAttachmentStack(gun, slot).getItem() != Items.AIR);
    }
    // Check if EXACT attachment installed in slot
    public boolean isAttachmentSpecific(ItemStack gun, String slot, Item attachment) {
        return (getAttachmentStack(gun, slot).getItem() == attachment);
    }
    // Get Item from attachment
    public Item getAttachmentItem(ItemStack gun, String type) {
        return getAttachmentStack(gun, type).getItem();
    }
    // Get attachment display name (Idk seems useless)
    public String getAttachmentName(ItemStack gun, String type) {
        return getAttachmentStack(gun, type).getDisplayName().getString();
    }

    // Weapon Tier system end
    public void OnCockEnd(Level pLevel, LivingEntity shooter, ItemStack gun, InteractionHand pUsedHand) { }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            return lazyAttributeMap.get();
        }
        return super.getAttributeModifiers(slot, stack);
    }

    // Cooldown stuff
    public void decreaseCooldownTick(ItemStack gun) {
        gun.getOrCreateTag().putInt("cooldownTicks", gun.getOrCreateTag().getInt("cooldownTicks")-1);

        if (gun.getOrCreateTag().getInt("cooldownTicks") < 0) {
            gun.getOrCreateTag().putInt("cooldownTicks", 0);
        }
    }

    public void setCooldown(Entity ent, ItemStack gun, int ticks) {
        if (ent instanceof Player ply) {
            ply.getCooldowns().addCooldown(gun.getItem(), ticks);
        }
        gun.getOrCreateTag().putInt("cooldownTicks", ticks);
    }

    public boolean checkCooldown(ItemStack gun) {
        return gun.getOrCreateTag().getInt("cooldownTicks") <= 0;
    }

    @Override
    public void inventoryTick(ItemStack pStack, Level pLevel, Entity pEntity, int pSlotId, boolean pIsSelected) {
        decreaseCooldownTick(pStack);
    }

    // Client stuff
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        // POSES
        consumer.accept(new IClientItemExtensions() {
            private static final HumanoidModel.ArmPose GUN_AIM = HumanoidModel.ArmPose.create("GUN_AIM", true, (model, entity, arm) -> {
                if (arm == HumanoidArm.RIGHT) {
                    model.rightArm.xRot = model.head.xRot - (float) Math.PI / 2F;
                    model.rightArm.yRot = model.head.yRot;

                    model.rightArm.x = -4;
                    model.rightArm.z = -1;

                    model.leftArm.xRot = model.head.xRot - (float) Math.PI / 2F;
                    model.leftArm.yRot = model.head.yRot / 2F + (float) Math.PI / 4F ;
                } else {
                    model.leftArm.xRot = model.head.xRot - (float) Math.PI / 2F;
                    model.leftArm.yRot = model.head.yRot;

                    model.leftArm.x = 4;
                    model.leftArm.z = -1;

                    model.rightArm.xRot = model.head.xRot - (float) Math.PI / 2F;
                    model.rightArm.yRot = model.head.yRot / 2F - (float) Math.PI / 4F ;
                }
            });

            private static final HumanoidModel.ArmPose GUN_RELOAD = HumanoidModel.ArmPose.create("GUN_RELOAD", true, (model, entity, arm) -> {
                if (arm == HumanoidArm.RIGHT) {
                    model.rightArm.xRot = (float) (-Math.PI*0.25F);
                    model.rightArm.yRot = (float) -(Math.PI*0.15F);
                    model.rightArm.zRot = (float) -(Math.PI*0.05F);

                    model.leftArm.xRot = (float) (-Math.PI*0.25F);
                    model.leftArm.yRot = (float) (Math.PI*0.25F);
                } else {
                    model.leftArm.xRot = (float) (-Math.PI*0.25F);
                    model.leftArm.yRot = (float) (Math.PI*0.15F);
                    model.leftArm.zRot = (float) (Math.PI*0.05F);

                    model.rightArm.xRot = (float) (-Math.PI*0.25F);
                    model.rightArm.yRot = (float) -(Math.PI*0.25F);
                }
            });

            // Pose setter
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
                if (!itemStack.isEmpty()) {
                    if (itemStack.getOrCreateTag().getBoolean("IsAiming")) {
                        return GUN_AIM;
                    } else {
                        return GUN_RELOAD;
                    }
                }
                return HumanoidModel.ArmPose.EMPTY;
            }
        });
    }

    // Set aiming animation
    public void setAimAnimation(ItemStack gun) {
        gun.getOrCreateTag().putBoolean("IsAiming", true);
    }
    // Set reloading animation
    public void setReloadAnimation(ItemStack gun) {
        gun.getOrCreateTag().putBoolean("IsAiming", false);
    }
    // Get should animate
    public boolean getShouldAim(ItemStack gun) {
        return gun.getOrCreateTag().getBoolean("IsAiming");
    }

    // Get ammo cooldown ticks
    public int ammoCooldown(LivingEntity ply, ItemStack gun) {
        return ammoCooldownTicks;
    }
    // Get shoot cooldown ticks
    public int shootCooldown(LivingEntity ply, ItemStack gun) {
        return shootCooldownTicks;
    }

    // Check if user should be able to press trigger (and try to shoot)
    public boolean allowPressingTrigger(Level pLevel, LivingEntity pPlayer, ItemStack gun, InteractionHand pUsedHand) {
        return true;
    }
    // Check if user should be abble to shoot (after pressing trigger)
    public boolean tryShoot(Level pLevel, LivingEntity pPlayer, ItemStack gun, InteractionHand pUsedHand) {
        return calculateHookSum("tryShoot", pPlayer, gun, 1) != 0;
    }

    // When try shoot failed
    public void onTryFailure(Level pLevel, LivingEntity pPlayer, ItemStack gunStack) {
    }

    // Get base NBT modifier
    public float getModifier(ItemStack gun, String modifierName) {
        if (!gun.getOrCreateTag().contains(modifierName)) {
            gun.getTag().putFloat(modifierName, 1.0F);
        }
        return gun.getTag().getFloat(modifierName);
    }
    // Increase/Decrease modifier
    public void multiplyModifier(ItemStack gun, String modifierName, float n) {
        float current = gun.getOrCreateTag().contains(modifierName) ? gun.getTag().getFloat(modifierName) : 1.0F;

        gun.getTag().putFloat(modifierName, current * n);
    }

    // Calculate propellant (projectile speed) modifier
    public float propellantModifier(LivingEntity shooter, ItemStack gun) {
        float baseValue = getModifier(gun, "propellantModifier");
        return calculateHookSum("calculatePropellantModifier", shooter, gun, baseValue);
    }

    // Calculate damage modifier
    public float damageModifier(LivingEntity shooter, ItemStack gun) {
        float baseValue = getModifier(gun, "damageModifier");
        return calculateHookSum("calculateDamageModifier", shooter, gun, baseValue);
    }

    // Calculate recoil X modifier
    public float recoilModifierX(LivingEntity id, ItemStack gun) {
        float baseValue = getModifier(gun, "recoilX");
        return calculateHookSum("calculateRecoilModifierX", id, gun, baseValue);
    }

    // Calculate recoil Y modifier
    public float recoilModifierY(LivingEntity id, ItemStack gun) {
        float baseValue = getModifier(gun, "recoilY");
        return calculateHookSum("calculateRecoilModifierY", id, gun, baseValue);
    }

    // Calculate accuracy modifier
    public float accuracyModifier(LivingEntity id, ItemStack gun) {
        float baseValue = getModifier(gun, "accuracy");
        return calculateHookSum("calculateAccuracyModifier", id, gun, baseValue);
    }

    // Yeah, I won't comment those
    public void multiplyPropellantModifier(ItemStack gun, float n) {
        multiplyModifier(gun, "propellantModifier", n);
    }
    public void multiplyDamageModifier(ItemStack gun, float n) {
        multiplyModifier(gun, "damageModifier", n);
    }
    public void multiplyRecoilModifierX(ItemStack gun, float n) {
        multiplyModifier(gun, "recoilModifierX", n);
    }
    public void multiplyRecoilModifierY(ItemStack gun, float n) {
        multiplyModifier(gun, "recoilModifierY", n);
    }
    public void multiplyAccuracyModifier(ItemStack gun, float n) {
        multiplyModifier(gun, "accuracyModifier", n);
    }

    // Shoot function (NOT ON HOOK, THIS IS INTERNAL CODE WITH ACTUAL SHOOTING)
    @ApiStatus.Internal
    public void shoot(Level pLevel, LivingEntity pPlayer, ItemStack gunStack, float rotationX, float rotationY) {
        triggerHooks("onShoot", pPlayer, gunStack);
        onShoot(rotationX,rotationY, pLevel, pPlayer, gunStack);
    }

    @ApiStatus.Internal
    public void shoot(Level pLevel, LivingEntity pPlayer, ItemStack gunStack) {
        shoot(pLevel, pPlayer, gunStack, CameraWork.getPlayerViewX(pPlayer),CameraWork.getPlayerViewY(pPlayer));
    }

    // On shoot function, called when shoot fired.
    public void onShoot(float rotationX, float rotationY, Level pLevel, LivingEntity shooter, ItemStack gunStack) {
        setCooldown(shooter, gunStack, shootCooldown(shooter, gunStack));
    }

    // When ammo put
    public void onAmmo(Level pLevel, LivingEntity shooter, ItemStack gun, ItemStack ammo ,InteractionHand pUsedHand) {
        setCooldown(shooter, gun, ammoCooldown(shooter, gun));
    }

    // Main Interaction, RMB
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


        return true;
    }

    @Override
    @ApiStatus.Internal
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        ItemStack gunStack = pPlayer.getItemInHand(pUsedHand);

        interaction(pLevel, pPlayer, gunStack, pUsedHand, false, 0, 0, null);

        return InteractionResultHolder.pass(pPlayer.getItemInHand(pUsedHand));
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        if (showTier) {
            if (this.getWeaponTier() == -1) {
                pTooltipComponents.add(Component.translatable("flintcore.weapontieruniversal"));
            } else {
                pTooltipComponents.add(Component.translatable("flintcore.weapontier").append(Integer.toString(this.getWeaponTier())));
            }
        }
        pTooltipComponents.add(Component.literal(""));

        int totalAttach = 0;
        for (String type : attachmentSlots) {
            if (isAttachmentValidAndEnabled(pStack, type)) {
                ItemStack item = getAttachmentStack(pStack, type);
                pTooltipComponents.add(Component.translatable("flintcore.attachment").append(item.getDisplayName()));
                item.getItem().appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);

                totalAttach++;
            }
        }
        if (totalAttach > 0) {
            pTooltipComponents.add(Component.literal(""));
        }


        if (!Screen.hasShiftDown()) {
            pTooltipComponents.add(Component.translatable("flintcore.guninfoshift"));
            pTooltipComponents.add(Component.literal(""));
        } else {
            pTooltipComponents.add(Component.translatable("flintcore.guninfoammo"));
            for (String ammo : allowedCalibersTags) {
                pTooltipComponents.add(Component.literal("   ").append(Component.translatable("flintcore.calibernames." + ammo)));
            }

            pTooltipComponents.add(Component.literal(""));

            if (!attachmentSlots.isEmpty()) {
                pTooltipComponents.add(Component.translatable("flintcore.guninfoattachmentslots"));
                for (String slot : attachmentSlots) {
                    pTooltipComponents.add(Component.literal("   ").append(Component.translatable("flintcore.slotnames." + slot)));
                }
                pTooltipComponents.add(Component.translatable("flintcore.guninfoattachmenttags"));
                for (String slot : this.allowedAttachmentsTags) {
                    pTooltipComponents.add(Component.literal("   ").append(Component.translatable("flintcore.attachmenttag." + slot)));
                }
            } else {
                pTooltipComponents.add(Component.translatable("flintcore.guninfonoattachment"));
            }

            pTooltipComponents.add(Component.literal(""));
        }
        //

        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

}
