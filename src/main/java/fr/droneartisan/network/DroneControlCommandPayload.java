package fr.droneartisan.network;

import fr.droneartisan.DroneArtisan;
import fr.droneartisan.entity.DroneTask;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DroneControlCommandPayload(int droneId, int taskId) implements CustomPacketPayload {
    public static final Type<DroneControlCommandPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DroneArtisan.MOD_ID, "drone_control_command"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DroneControlCommandPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, DroneControlCommandPayload::droneId,
                    ByteBufCodecs.VAR_INT, DroneControlCommandPayload::taskId, DroneControlCommandPayload::new);

    public DroneTask task() {
        return taskId >= 0 && taskId < DroneTask.values().length ? DroneTask.values()[taskId] : null;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
