package com.helicoptermod.item;

import com.helicoptermod.entity.HelicopterEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Right-click a block with this item to place the helicopter, the same way a boat item works.
 * Breaking a placed (empty) helicopter drops this item again — see
 * {@link HelicopterEntity#breakAndDropItem()}.
 */
public class HelicopterItem extends Item {

    private final EntityType<HelicopterEntity> entityType;

    public HelicopterItem(EntityType<HelicopterEntity> entityType, Settings settings) {
        super(settings);
        this.entityType = entityType;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        BlockPos placePos = context.getBlockPos().offset(context.getSide());
        PlayerEntity player = context.getPlayer();
        float yaw = player != null ? player.getYaw() : 0.0f;

        HelicopterEntity helicopter = new HelicopterEntity(this.entityType, world);
        helicopter.refreshPositionAndAngles(
                placePos.getX() + 0.5,
                placePos.getY() + 0.35,
                placePos.getZ() + 0.5,
                yaw,
                0.0f
        );

        if (!world.spawnEntity(helicopter)) {
            return ActionResult.FAIL;
        }

        if (player != null && !player.getAbilities().creativeMode) {
            context.getStack().decrement(1);
        }
        return ActionResult.SUCCESS;
    }
}
