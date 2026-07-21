package com.helicoptermod.entity;

import com.helicoptermod.item.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.damage.DamageSource;
import org.jetbrains.annotations.Nullable;

/**
 * A small, unarmed, easy-to-fly light transport helicopter.
 * <p>
 * Design notes (see the README in the mod zip for the full explanation):
 * <ul>
 *   <li>No gravity, ever. The helicopter simply holds still in the air with no input. This is what
 *       makes it "impossible to crash from falling" and what makes it stay exactly where you left it.</li>
 *   <li>No fuel system (the spec allowed skipping it, and it keeps this mod much simpler).</li>
 *   <li>Movement is fully arcade: velocity smoothly chases a target velocity every tick. There is no
 *       momentum-based "real" flight simulation.</li>
 *   <li>Yaw (facing) is a blend of A/D (direct turning) and where the pilot is looking (a gentle
 *       homing turn), so both control schemes described in the spec work together instead of fighting.</li>
 *   <li>Right-click the helicopter again while seated to get out cleanly. Minecraft also dismounts
 *       riders whenever Sneak is pressed (that is a built-in engine behaviour tied to the same key we
 *       use for "descend", not something this mod can cleanly turn off without a fragile mixin) — so as
 *       a safety net, anyone who leaves this helicopter mid-air gets a few seconds of Slow Falling so an
 *       accidental exit is never a punishing fall.</li>
 * </ul>
 */
public class HelicopterEntity extends Entity {

    // ---- Seats: index 0 is the pilot (controls flight); 1-3 are along-for-the-ride passengers. ----
    private static final double[] SEAT_FORWARD = {0.6, -0.2, -0.2, -0.9};
    private static final double[] SEAT_SIDEWAYS = {0.0, -0.55, 0.55, 0.0};
    private static final double SEAT_HEIGHT = 0.95;

    // ---- Flight feel (arcade, not simulated) ----
    private static final float MAX_HORIZONTAL_SPEED = 0.7f;   // blocks/tick
    private static final float MAX_VERTICAL_SPEED = 0.35f;    // blocks/tick
    private static final float HORIZONTAL_ACCEL = 0.12f;      // 0-1, how fast velocity chases the target
    private static final float VERTICAL_ACCEL = 0.18f;
    private static final float IDLE_DRAG = 0.10f;             // how fast an unpiloted helicopter settles to a stop
    private static final float REVERSE_SPEED_FACTOR = 0.5f;   // S is slower than W, like a real light heli
    private static final float TURN_SPEED_PER_TICK = 2.5f;    // degrees, direct A/D turning
    private static final float MOUSE_YAW_HOMING = 0.06f;      // fraction of the look-direction gap closed per tick
    // If A and D ever feel swapped in your build, flip this to -1.0f (small mapping differences can
    // change the sign of sidewaysSpeed between versions and we can't compile-test that here).
    private static final float TURN_INPUT_SIGN = 1.0f;

    private static final float ROTOR_SPEED_FLYING = 42.0f;    // degrees/tick
    private static final float ROTOR_SPEED_IDLE = 6.0f;

    private static final float MAX_HEALTH = 60.0f;
    private static final String NBT_HEALTH = "Health";

    public float prevRotorRotation;
    public float rotorRotation;
    /**
     * Our own tracked "yaw last tick", used only for smooth render interpolation.
     * We keep this instead of relying on a base-class field name we can't verify without
     * compiling against the real 1.21.11 jar.
     */
    public float prevYawForRender;
    private float health = MAX_HEALTH;

    public HelicopterEntity(EntityType<? extends HelicopterEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
        // Stagger idle rotor phase per-instance so several parked helicopters don't spin in lockstep.
        this.rotorRotation = this.random.nextFloat() * 360.0f;
        this.prevRotorRotation = this.rotorRotation;
        this.prevYawForRender = this.getYaw();
    }

    // ------------------------------------------------------------------------------------------
    // Core tick / flight logic
    // ------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        this.prevRotorRotation = this.rotorRotation;
        this.prevYawForRender = this.getYaw();

        LivingEntity pilot = this.getControllingPassenger();
        if (pilot != null) {
            this.flyWithInput(pilot);
            this.rotorRotation += ROTOR_SPEED_FLYING;
        } else {
            this.hoverIdle();
            this.rotorRotation += ROTOR_SPEED_IDLE;
        }

        this.move(MovementType.SELF, this.getVelocity());
        this.stopVelocityOnCollision();

        if (this.getEntityWorld().isClient) {
            this.spawnFlightParticles(pilot != null);
        }
    }

    /** Reads the pilot's WASD/Space/Shift/mouse input and turns it into smooth target velocity + yaw. */
    private void flyWithInput(LivingEntity pilot) {
        float forwardInput = pilot.forwardSpeed;              // W = positive, S = negative
        float turnInput = pilot.sidewaysSpeed * TURN_INPUT_SIGN; // A/D, repurposed here as "turn" not "strafe"
        boolean ascendInput = pilot.isJumping();               // Space
        boolean descendInput = pilot.isSneaking();              // Shift

        // Yaw: A/D turns immediately; looking around also gently "homes" the nose that direction over
        // a second or two, so both control descriptions in the spec are satisfied without fighting.
        float yawGap = MathHelper.wrapDegrees(pilot.getYaw() - this.getYaw());
        float newYaw = this.getYaw() + yawGap * MOUSE_YAW_HOMING + turnInput * TURN_SPEED_PER_TICK;
        this.setYaw(newYaw);
        this.setPitch(0.0f);

        float throttle = forwardInput >= 0.0f ? forwardInput : forwardInput * REVERSE_SPEED_FACTOR;
        double yawRad = Math.toRadians(this.getYaw());
        double dirX = -Math.sin(yawRad);
        double dirZ = Math.cos(yawRad);

        double targetX = dirX * throttle * MAX_HORIZONTAL_SPEED;
        double targetZ = dirZ * throttle * MAX_HORIZONTAL_SPEED;

        double targetY;
        if (ascendInput == descendInput) {
            targetY = 0.0; // neither, or both (they cancel out) -> hold altitude, i.e. auto-hover
        } else {
            targetY = ascendInput ? MAX_VERTICAL_SPEED : -MAX_VERTICAL_SPEED;
        }

        Vec3d v = this.getVelocity();
        this.setVelocity(
                MathHelper.lerp(HORIZONTAL_ACCEL, v.x, targetX),
                MathHelper.lerp(VERTICAL_ACCEL, v.y, targetY),
                MathHelper.lerp(HORIZONTAL_ACCEL, v.z, targetZ)
        );
        this.velocityDirty = true;
    }

    /** No pilot: smoothly bleed off any remaining velocity and just hang there (no gravity to fight). */
    private void hoverIdle() {
        Vec3d v = this.getVelocity();
        if (v.lengthSquared() < 1.0E-5) {
            if (!v.equals(Vec3d.ZERO)) {
                this.setVelocity(Vec3d.ZERO);
            }
            return;
        }
        this.setVelocity(
                MathHelper.lerp(IDLE_DRAG, v.x, 0.0),
                MathHelper.lerp(IDLE_DRAG, v.y, 0.0),
                MathHelper.lerp(IDLE_DRAG, v.z, 0.0)
        );
    }

    /** Stop dead (instead of jittering) on collision, rather than simulating a crash. */
    private void stopVelocityOnCollision() {
        if (!this.horizontalCollision && !this.verticalCollision) {
            return;
        }
        Vec3d v = this.getVelocity();
        this.setVelocity(
                this.horizontalCollision ? 0.0 : v.x,
                this.verticalCollision ? 0.0 : v.y,
                this.horizontalCollision ? 0.0 : v.z
        );
    }

    private void spawnFlightParticles(boolean flying) {
        if (!flying || this.random.nextInt(4) != 0) {
            return;
        }
        double px = this.getX() + (this.random.nextDouble() - 0.5) * 1.6;
        double py = this.getY() - 0.1;
        double pz = this.getZ() + (this.random.nextDouble() - 0.5) * 1.6;
        this.getEntityWorld().addParticle(ParticleTypes.CLOUD, px, py, pz, 0.0, -0.02, 0.0);
    }

    // ------------------------------------------------------------------------------------------
    // Riding: mounting, seats, dismount safety net
    // ------------------------------------------------------------------------------------------

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (this.getPassengerList().contains(player)) {
            if (!this.getEntityWorld().isClient) {
                player.stopRiding();
            }
            return ActionResult.SUCCESS;
        }
        if (!this.canAddPassenger(player)) {
            return ActionResult.PASS;
        }
        if (!this.getEntityWorld().isClient) {
            player.startRiding(this);
        }
        return ActionResult.SUCCESS;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity first = this.getFirstPassenger();
        return first instanceof LivingEntity livingEntity ? livingEntity : null;
    }

    @Override
    public boolean canAddPassenger(Entity passenger) {
        return this.getPassengerList().size() < SEAT_FORWARD.length;
    }

    @Override
    protected void updatePassengerPosition(Entity passenger, PositionUpdater positionUpdater) {
        if (!this.hasPassenger(passenger)) {
            return;
        }
        int index = MathHelper.clamp(this.getPassengerList().indexOf(passenger), 0, SEAT_FORWARD.length - 1);

        double yawRad = Math.toRadians(this.getYaw());
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);
        double forward = SEAT_FORWARD[index];
        double sideways = SEAT_SIDEWAYS[index];

        double worldX = this.getX() + (sideways * cos - forward * sin);
        double worldZ = this.getZ() + (forward * cos + sideways * sin);
        double worldY = this.getY() + SEAT_HEIGHT;

        positionUpdater.accept(passenger, worldX, worldY, worldZ);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (this.getEntityWorld().isClient || !(passenger instanceof LivingEntity livingEntity)) {
            return;
        }
        if (!livingEntity.isOnGround()) {
            // Safety net described in the class javadoc above: never let leaving this helicopter
            // (however that happened) turn into fall damage.
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 100, 0, true, false));
        }
    }

    // ------------------------------------------------------------------------------------------
    // Damage / breaking back into an item, and picking it back up
    // ------------------------------------------------------------------------------------------

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (this.isRemoved() || this.isInvulnerableTo(world, source)) {
            return false;
        }

        this.health -= amount;
        this.velocityDirty = true;

        if (this.health <= 0.0f) {
            this.breakAndDropItem();
        }
        return true;
    }

    private void breakAndDropItem() {
        if (!this.getEntityWorld().isClient) {
            ItemEntity itemEntity = new ItemEntity(this.getEntityWorld(), this.getX(), this.getY(), this.getZ(),
                    new ItemStack(ModItems.HELICOPTER));
            itemEntity.setVelocity(Vec3d.ZERO);
            this.getEntityWorld().spawnEntity(itemEntity);
        }
        this.discard();
    }

    // ------------------------------------------------------------------------------------------
    // Boilerplate required by Entity
    // ------------------------------------------------------------------------------------------

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        // No synced fields needed: position/rotation/passengers are already synced by the base
        // Entity class, and rotor spin + flying-or-not are derived client-side from that.
    }

    @Override
    protected void readCustomData(ReadView view) {
        this.health = view.getFloat(NBT_HEALTH).orElse(MAX_HEALTH);
    }

    @Override
    protected void writeCustomData(WriteView view) {
        view.putFloat(NBT_HEALTH, this.health);
    }
}
