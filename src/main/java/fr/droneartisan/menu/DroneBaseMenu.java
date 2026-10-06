package fr.droneartisan.menu;

import fr.droneartisan.block.DroneBaseBlockEntity;
import fr.droneartisan.entity.DroneEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class DroneBaseMenu extends AbstractContainerMenu {
    private final BlockPos basePos;
    private final int droneId;

    public DroneBaseMenu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        this(containerId, inventory, data.readBlockPos(), data.readVarInt());
    }

    public DroneBaseMenu(int containerId, Inventory inventory, BlockPos basePos, DroneEntity drone) {
        this(containerId, inventory, basePos, drone.getId());
    }

    private DroneBaseMenu(int containerId, Inventory inventory, BlockPos basePos, int droneId) {
        super(DroneBaseMenuRegistry.BASE_CONTROL.get(), containerId);
        this.basePos = basePos;
        this.droneId = droneId;
    }

    public DroneEntity drone(Player player) {
        return player.level().getEntity(droneId) instanceof DroneEntity drone ? drone : null;
    }

    public BlockPos basePos() {
        return basePos;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.distanceToSqr(basePos.getCenter()) > 64.0 || !player.level().hasChunkAt(basePos)) return false;
        if (player.level().isClientSide) {
            return player.level().getBlockEntity(basePos) instanceof DroneBaseBlockEntity && drone(player) != null;
        }
        if (!(player.level().getBlockEntity(basePos) instanceof DroneBaseBlockEntity base)
                || !base.isOwner(player)) return false;
        DroneEntity drone = drone(player);
        return drone != null && drone.isAlive() && drone.isOwnedBy(player)
                && drone.isBasedAt(basePos) && drone.getUUID().equals(base.droneId());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    public static final class Provider implements net.minecraft.world.MenuProvider {
        private final BlockPos basePos;
        private final DroneEntity drone;

        public Provider(BlockPos basePos, DroneEntity drone) {
            this.basePos = basePos.immutable();
            this.drone = drone;
        }

        @Override
        public net.minecraft.network.chat.Component getDisplayName() {
            return net.minecraft.network.chat.Component.translatable("container.droneartisan.base_control");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new DroneBaseMenu(containerId, inventory, basePos, drone);
        }
    }
}
