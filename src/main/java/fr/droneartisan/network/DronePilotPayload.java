package fr.droneartisan.network;

import fr.droneartisan.DroneArtisan;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DronePilotPayload(int droneId, float forward, float strafe, float vertical,
                                float yaw, float pitch, boolean sprinting, int action, BlockPos blockPos,
                                int face, int entityId) implements CustomPacketPayload {
    public static final int NONE = DronePilotControls.NO_ACTION;
    public static final int MINE = 1;
    public static final int INTERACT_BLOCK = 2;
    public static final int LASER = 3;
    public static final int INTERACT_ENTITY = 4;
    public static final int START_PILOT = 5;
    public static final int STOP_PILOT = 6;
    public static final Type<DronePilotPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(DroneArtisan.MOD_ID, "drone_pilot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DronePilotPayload> STREAM_CODEC =
            StreamCodec.of(DronePilotPayload::write, DronePilotPayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, DronePilotPayload payload) {
        buffer.writeVarInt(payload.droneId);
        buffer.writeFloat(payload.forward);
        buffer.writeFloat(payload.strafe);
        buffer.writeFloat(payload.vertical);
        buffer.writeFloat(payload.yaw);
        buffer.writeFloat(payload.pitch);
        buffer.writeBoolean(payload.sprinting);
        buffer.writeVarInt(payload.action);
        buffer.writeBlockPos(payload.blockPos);
        buffer.writeVarInt(payload.face);
        buffer.writeVarInt(payload.entityId);
    }

    private static DronePilotPayload read(RegistryFriendlyByteBuf buffer) {
        return new DronePilotPayload(buffer.readVarInt(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readBoolean(),
                buffer.readVarInt(), buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt());
    }

    public boolean hasValidControls() {
        return DronePilotControls.isValid(forward, strafe, vertical, yaw, pitch, action, face);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
