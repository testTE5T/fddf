package com.helicoptermod.client;

import com.helicoptermod.entity.HelicopterEntity;
import com.helicoptermod.sound.ModSounds;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.sound.SoundCategory;

/**
 * The basic engine sound, requested in the spec. One looping instance is created per flying
 * helicopter by {@code HelicopterModClient}, which also stops it again once nobody is piloting.
 */
public class HelicopterEngineSoundInstance extends MovingSoundInstance {

    private final HelicopterEntity helicopter;

    public HelicopterEngineSoundInstance(HelicopterEntity helicopter) {
        super(ModSounds.HELICOPTER_ENGINE, SoundCategory.NEUTRAL, helicopter.getRandom());
        this.helicopter = helicopter;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.7f;
        this.x = helicopter.getX();
        this.y = helicopter.getY();
        this.z = helicopter.getZ();
    }

    @Override
    public void tick() {
        if (!this.helicopter.isAlive() || this.helicopter.getControllingPassenger() == null) {
            this.setDone();
            return;
        }
        this.x = this.helicopter.getX();
        this.y = this.helicopter.getY();
        this.z = this.helicopter.getZ();
    }
}
