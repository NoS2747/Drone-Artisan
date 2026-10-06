package fr.droneartisan.network;

import fr.droneartisan.block.DroneBaseBlockEntity;
import fr.droneartisan.entity.DroneEntity;
import fr.droneartisan.entity.DroneTask;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class DroneBaseNetwork {
    private DroneBaseNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(DroneBaseCommandPayload.TYPE, DroneBaseCommandPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player)
                            || !player.serverLevel().hasChunkAt(payload.basePos())
                            || player.distanceToSqr(payload.basePos().getCenter()) > 64.0
                            || !(player.serverLevel().getBlockEntity(payload.basePos())
                            instanceof DroneBaseBlockEntity base)
                            || !base.isOwner(player) || base.droneId() == null) return;
                    DroneTask task = payload.task();
                    if (task == null) return;
                    if (player.serverLevel().getEntity(base.droneId()) instanceof DroneEntity drone
                            && drone.isAlive() && drone.isOwnedBy(player)
                            && drone.isBasedAt(payload.basePos())
                            && drone.distanceToSqr(payload.basePos().getCenter()) <= 128.0 * 128.0) {
                        drone.setTask(task);
                    }
                }));
    }
}
