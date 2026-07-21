package com.helicoptermod.client;

import com.helicoptermod.HelicopterMod;
import com.helicoptermod.entity.HelicopterEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class HelicopterEntityRenderer extends EntityRenderer<HelicopterEntity, HelicopterRenderState> {

    private static final Identifier TEXTURE =
            Identifier.of(HelicopterMod.MOD_ID, "textures/entity/helicopter/helicopter.png");

    private final HelicopterEntityModel model;

    public HelicopterEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new HelicopterEntityModel(context.getPart(HelicopterModClient.HELICOPTER_MODEL_LAYER));
        this.shadowRadius = 0.9f;
    }

    @Override
    public HelicopterRenderState createRenderState() {
        return new HelicopterRenderState();
    }

    @Override
    public void updateRenderState(HelicopterEntity entity, HelicopterRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);

        state.bodyYaw = MathHelper.lerp(tickDelta, entity.prevYawForRender, entity.getYaw());
        state.rotorRotation = MathHelper.lerp(tickDelta, entity.prevRotorRotation, entity.rotorRotation) % 360.0f;
        state.flying = entity.getControllingPassenger() != null;

        // Small cosmetic bank/tilt so it doesn't look totally rigid in flight. Visual only —
        // none of this feeds back into actual movement.
        double speed = entity.getVelocity().horizontalLength();
        state.pitchDegrees = (float) MathHelper.clamp(speed * -14.0, -9.0, 9.0);
        float yawDelta = MathHelper.wrapDegrees(entity.getYaw() - entity.prevYawForRender);
        state.rollDegrees = MathHelper.clamp(-yawDelta * 2.2f, -12.0f, 12.0f);
    }

    @Override
    public void render(HelicopterRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.translate(0.0, 0.05, 0.0); // tiny lift so skids don't z-fight with the ground block
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - state.bodyYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(state.pitchDegrees));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(state.rollDegrees));

        this.model.setAngles(state);

        var vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(this.getTexture(state)));
        // NOTE: EntityModel#render's exact parameter list has shifted a little across 1.21.x
        // snapshots (some versions add/remove a packed-colour int here). If this line doesn't
        // compile as-is, check EntityModel in your decompiled sources and adjust accordingly —
        // it's a one-line fix.
        this.model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, -1);

        matrices.pop();
        super.render(state, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(HelicopterRenderState state) {
        return TEXTURE;
    }
}
