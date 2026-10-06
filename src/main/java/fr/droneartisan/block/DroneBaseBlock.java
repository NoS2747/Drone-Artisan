package fr.droneartisan.block;

import fr.droneartisan.entity.DroneEntity;
import fr.droneartisan.menu.DroneBaseMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import com.mojang.serialization.MapCodec;

public final class DroneBaseBlock extends BaseEntityBlock {
    public static final MapCodec<DroneBaseBlock> CODEC = simpleCodec(DroneBaseBlock::new);

    public DroneBaseBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DroneBaseBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof DroneBaseBlockEntity base) {
            base.claim(player);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel)
                || !(serverLevel.getBlockEntity(pos) instanceof DroneBaseBlockEntity base)) {
            return InteractionResult.PASS;
        }
        if (base.ownerId() == null) base.claim(player);
        if (!base.isOwner(player)) {
            player.displayClientMessage(Component.translatable("message.droneartisan.base_not_yours"), true);
            return InteractionResult.CONSUME;
        }

        DroneEntity drone = base.droneId() == null ? null
                : serverLevel.getEntity(base.droneId()) instanceof DroneEntity linked ? linked : null;
        if (drone == null || !drone.isOwnedBy(player) || !drone.isAlive()) {
            drone = serverLevel.getEntitiesOfClass(DroneEntity.class,
                            AABB.ofSize(pos.getCenter(), 128.0, 128.0, 128.0),
                            candidate -> candidate.isOwnedBy(player) && candidate.isAlive())
                    .stream().min(java.util.Comparator.comparingDouble(candidate ->
                            candidate.distanceToSqr(pos.getCenter()))).orElse(null);
            if (drone == null) {
                player.displayClientMessage(Component.translatable("message.droneartisan.base_no_drone"), true);
                return InteractionResult.CONSUME;
            }
            base.bind(drone);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            DroneEntity boundDrone = drone;
            serverPlayer.openMenu(new DroneBaseMenu.Provider(pos, boundDrone),
                    buffer -> {
                        buffer.writeBlockPos(pos);
                        buffer.writeVarInt(boundDrone.getId());
                    });
        }
        return InteractionResult.CONSUME;
    }
}
