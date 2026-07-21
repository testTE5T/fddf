package com.helicoptermod.client;

import net.minecraft.client.render.entity.state.EntityRenderState;

/**
 * Per-frame render data for the helicopter, filled in by
 * {@link HelicopterEntityRenderer#updateRenderState}. Keeping this separate from
 * {@code HelicopterEntity} is what lets the renderer run safely off the render thread.
 */
public class HelicopterRenderState extends EntityRenderState {
    /** Interpolated yaw, in degrees, used to orient the whole model. */
    public float bodyYaw;
    /** Interpolated rotor spin angle, in degrees. */
    public float rotorRotation;
    /** Whether a pilot is currently flying it (purely cosmetic: rotor speed, engine sound). */
    public boolean flying;
    /** Small cosmetic nose-tilt based on speed. Purely visual, does not affect movement. */
    public float pitchDegrees;
    /** Small cosmetic bank/roll based on turn rate. Purely visual, does not affect movement. */
    public float rollDegrees;
}
