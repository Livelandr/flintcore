package com.livelandr.flintcore.core.sounds.TickableSounds;

import com.livelandr.flintcore.Flintcore;
import com.livelandr.flintcore.core.sounds.ModSoundDeferred;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

import java.util.logging.Logger;

public class MortarFallingSoundInstance extends AbstractTickableSoundInstance {
    public Entity parentEntity;
    double maxDistance = 48;
    boolean whistlePlayed = false;

    public MortarFallingSoundInstance(SoundEvent soundEvent, double x, double y, double z) {
        super(soundEvent, SoundSource.NEUTRAL, RandomSource.create());

        this.looping = true;
        this.delay = 0;

        this.x = x;
        this.y = y;
        this.z = z;

        this.volume = 4.0F;
        this.pitch = 3.0F;
    }

    public void setEnt(Entity parent) {
        parentEntity = parent;
    }

    public void setPosition(double X, double Y, double Z) {
        this.x = X;
        this.y = Y;
        this.z = Z;
    }

    @Override
    public void tick() {
        if (parentEntity == null || parentEntity.isRemoved()) {
            this.stop();
        }
        LocalPlayer player = Minecraft.getInstance().player;

        double dx = this.getX() - player.getX();
        double dy = this.getY() - player.getY();
        double dz = this.getZ() - player.getZ();

        double dist = Math.sqrt(dx*dx+dy*dy+dz*dz);
        double distInterpolation = dist/maxDistance;

        this.pitch = 0.5F + (float) distInterpolation;

        setPosition(parentEntity.getX(),parentEntity.getY(),parentEntity.getZ());
    }
}
