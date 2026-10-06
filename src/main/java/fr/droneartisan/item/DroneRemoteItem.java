package fr.droneartisan.item;

import fr.droneartisan.entity.DroneEntity;
import fr.droneartisan.menu.DroneControlMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DroneRemoteItem extends Item {
    public DroneRemoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                   net.minecraft.world.InteractionHand hand) {
        if (!(target instanceof DroneEntity drone) || !drone.isOwnedBy(player)) return InteractionResult.PASS;
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        openControl(player, drone);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
                                                   net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);
        DroneEntity drone = DroneEntity.findOwned(player);
        if (drone == null) {
            player.displayClientMessage(Component.translatable("message.droneartisan.no_drone"), true);
            return InteractionResultHolder.fail(stack);
        }
        openControl(player, drone);
        return InteractionResultHolder.consume(stack);
    }

    public void openControl(Player player, DroneEntity drone) {
        if (player instanceof ServerPlayer serverPlayer && drone.isOwnedBy(player)
                && player.distanceToSqr(drone) <= 128.0 * 128.0) {
            serverPlayer.openMenu(new DroneControlMenu.Provider(drone),
                    buffer -> buffer.writeVarInt(drone.getId()));
        }
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        if (context.getLevel().isClientSide() || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        DroneEntity drone = DroneEntity.findOwned(player);
        if (drone == null) {
            player.displayClientMessage(Component.translatable("message.droneartisan.no_drone"), true);
            return InteractionResult.FAIL;
        }
        if (!drone.placeBlock(player, context.getClickedPos(), context.getClickedFace(), context.getItemInHand())) {
            player.displayClientMessage(Component.translatable("message.droneartisan.no_safe_block"), true);
            return InteractionResult.FAIL;
        }
        return InteractionResult.CONSUME;
    }
}
