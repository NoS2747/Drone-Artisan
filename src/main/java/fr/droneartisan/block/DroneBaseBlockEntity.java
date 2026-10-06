package fr.droneartisan.block;

import fr.droneartisan.entity.DroneEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public final class DroneBaseBlockEntity extends BlockEntity {
    private UUID ownerId;
    private UUID droneId;

    public DroneBaseBlockEntity(BlockPos pos, BlockState state) {
        super(DroneBaseRegistry.DRONE_BASE_ENTITY.get(), pos, state);
    }

    public UUID ownerId() {
        return ownerId;
    }

    public UUID droneId() {
        return droneId;
    }

    public boolean isOwner(Player player) {
        return ownerId != null && ownerId.equals(player.getUUID());
    }

    public void claim(Player player) {
        if (ownerId == null) {
            ownerId = player.getUUID();
            setChanged();
        }
    }

    public void bind(DroneEntity drone) {
        droneId = drone.getUUID();
        drone.setHomePos(worldPosition);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        if (droneId != null) tag.putUUID("Drone", droneId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        droneId = tag.hasUUID("Drone") ? tag.getUUID("Drone") : null;
    }
}
