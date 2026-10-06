package fr.droneartisan.network;

import fr.droneartisan.entity.DroneEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class DronePilotNetwork {
    private DronePilotNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(DronePilotPayload.TYPE, DronePilotPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player)
                            || !payload.hasValidControls()
                            || !(player.serverLevel().getEntity(payload.droneId()) instanceof DroneEntity drone)
                            || !drone.isAlive() || !drone.isOwnedBy(player)
                            || player.distanceToSqr(drone) > 256.0 * 256.0
                            || payload.action() == DronePilotPayload.START_PILOT
                            && (player.distanceToSqr(drone) > 128.0 * 128.0
                            || drone.getBattery() <= 0 || !drone.isPilotConsoleOpen(player))
                            || payload.action() != DronePilotPayload.START_PILOT
                            && !drone.isBeingPilotedBy(player)) return;
                    drone.applyPilotInput(player, payload);
                }));
    }
}
