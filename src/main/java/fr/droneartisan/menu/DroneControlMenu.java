package fr.droneartisan.menu;

import fr.droneartisan.entity.DroneEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class DroneControlMenu extends AbstractContainerMenu {
    private final int droneId;
    private final DroneEntity drone;

    public DroneControlMenu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        this(containerId, inventory, inventory.player.level().getEntity(data.readVarInt()) instanceof DroneEntity found
                ? found : null);
    }

    public DroneControlMenu(int containerId, Inventory inventory, DroneEntity drone) {
        super(DroneControlMenuRegistry.DRONE_CONTROL.get(), containerId);
        this.drone = drone;
        droneId = drone == null ? -1 : drone.getId();
        checkContainerSize(drone == null ? new net.minecraft.world.SimpleContainer(9) : drone.getCargo(), 9);
        net.minecraft.world.Container cargo = drone == null
                ? new net.minecraft.world.SimpleContainer(9) : drone.getCargo();
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new Slot(cargo, slot, 8 + slot * 18, 132));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 154 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 212));
        }
    }

    public DroneEntity drone() {
        return drone;
    }

    @Override
    public boolean stillValid(Player player) {
        return drone != null && drone.isAlive() && drone.isOwnedBy(player)
                && player.distanceToSqr(drone) <= 128.0 * 128.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (slotIndex < 9) {
            if (!moveItemStackTo(stack, 9, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 9, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    public static final class Provider implements net.minecraft.world.MenuProvider {
        private final DroneEntity drone;

        public Provider(DroneEntity drone) {
            this.drone = drone;
        }

        @Override
        public net.minecraft.network.chat.Component getDisplayName() {
            return net.minecraft.network.chat.Component.translatable("container.droneartisan.drone_control");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new DroneControlMenu(containerId, inventory, drone);
        }
    }
}
