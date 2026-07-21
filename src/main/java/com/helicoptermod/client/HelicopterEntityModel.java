package com.helicoptermod.client;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModel;

/**
 * Simple, low-poly helicopter model — one static "body" part (fuselage, tail boom, tail fin,
 * rotor mast, skids, struts) plus two separately-pivoted parts that spin: the main rotor and the
 * tail rotor.
 * <p>
 * Texture layout: the UV coordinates below are pre-computed to exactly match
 * {@code assets/helicoptermod/textures/entity/helicopter/helicopter.png} (92x208). If you resize
 * or re-lay-out that texture, update the {@code .uv(...)} calls to match, or things will look
 * subtly wrong (wrong-coloured faces) rather than crash.
 */
public class HelicopterEntityModel extends EntityModel<HelicopterRenderState> {

    private static final int TEXTURE_WIDTH = 92;
    private static final int TEXTURE_HEIGHT = 208;

    private final ModelPart mainRotor;
    private final ModelPart tailRotor;

    public HelicopterEntityModel(ModelPart root) {
        super(root);
        this.mainRotor = root.getChild("main_rotor");
        this.tailRotor = root.getChild("tail_rotor");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        // Static airframe: fuselage, tail boom, tail fin, rotor mast, both skids, four struts.
        root.addChild("body", ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-8, -24, -15, 16, 14, 30)      // fuselage / cabin
                        .uv(0, 44).cuboid(-3, -20, 11, 6, 6, 22)        // tail boom
                        .uv(0, 72).cuboid(-1, -28, 27, 2, 8, 6)         // tail fin
                        .uv(0, 106).cuboid(-2, -30, -2, 4, 6, 4)        // main rotor mast
                        .uv(0, 162).cuboid(-8, -2, -13, 2, 2, 34)       // left skid
                        .uv(0, 162).cuboid(6, -2, -13, 2, 2, 34)        // right skid
                        .uv(0, 198).cuboid(-7, -10, -10, 2, 8, 2)       // strut, front-left
                        .uv(0, 198).cuboid(5, -10, -10, 2, 8, 2)        // strut, front-right
                        .uv(0, 198).cuboid(-7, -10, 6, 2, 8, 2)         // strut, back-left
                        .uv(0, 198).cuboid(5, -10, 6, 2, 8, 2),         // strut, back-right
                ModelTransform.pivot(0, 0, 0));

        // Main rotor: two-blade bar through the mast top, spins around Y (yaw).
        root.addChild("main_rotor", ModelPartBuilder.create()
                        .uv(0, 116).cuboid(-1, -1, -22, 2, 2, 44),
                ModelTransform.pivot(0, -31, 0));

        // Tail rotor: small disc on the side of the fin, spins around X (pitch).
        root.addChild("tail_rotor", ModelPartBuilder.create()
                        .uv(0, 86).cuboid(-0.5f, -5, -5, 1, 10, 10),
                ModelTransform.pivot(2.5f, -24, 32));

        return TexturedModelData.of(modelData, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    public void setAngles(HelicopterRenderState state) {
        float radians = (float) Math.toRadians(state.rotorRotation);
        this.mainRotor.yaw = radians;
        // Tail rotor spins a bit faster for visual variety; it's a much smaller disc.
        this.tailRotor.pitch = radians * 3.0f;
    }
}
