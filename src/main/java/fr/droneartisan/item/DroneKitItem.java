package fr.droneartisan.item;

import fr.droneartisan.entity.DroneEntity;
import fr.droneartisan.entity.DroneRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class DroneKitItem extends Item {
    public DroneKitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.FAIL;
        DroneEntity drone = DroneRegistry.DRONE.get().create(level);
        if (drone == null) return InteractionResult.FAIL;
        if (context.getPlayer() == null) return InteractionResult.FAIL;
        drone.setOwner(context.getPlayer().getUUID());
        drone.moveTo(context.getClickedPos().above(), context.getRotation(), 0.0F);
        if (!level.addFreshEntity(drone)) return InteractionResult.FAIL;
        if (!context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
            context.getPlayer().displayClientMessage(Component.translatable("message.droneartisan.deployed"), true);
        }
        return InteractionResult.CONSUME;
    }
}
