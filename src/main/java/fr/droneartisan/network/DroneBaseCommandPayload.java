package fr.droneartisan.network;

import fr.droneartisan.DroneArtisan;
import fr.droneartisan.entity.DroneTask;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DroneBaseCommandPayload(BlockPos basePos, int taskId) implements CustomPacketPayload {
    public static final Type<DroneBaseCommandPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DroneArtisan.MOD_ID, "base_command"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DroneBaseCommandPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, DroneBaseCommandPayload::basePos,
                    ByteBufCodecs.VAR_INT, DroneBaseCommandPayload::taskId, DroneBaseCommandPayload::new);

    public DroneTask task() {
        return taskId >= 0 && taskId < DroneTask.values().length ? DroneTask.values()[taskId] : null;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
