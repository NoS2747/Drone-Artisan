package fr.droneartisan.network;

import fr.droneartisan.entity.DroneEntity;
import fr.droneartisan.entity.DroneTask;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class DroneControlNetwork {
    private DroneControlNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(DroneControlCommandPayload.TYPE,
                DroneControlCommandPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player)
                            || !(player.serverLevel().getEntity(payload.droneId()) instanceof DroneEntity drone)
                            || !drone.isAlive() || !drone.isOwnedBy(player)
                            || player.distanceToSqr(drone) > 128.0 * 128.0
                            || !isRemoteOpen(player) || payload.task() == null) return;
                    drone.setTask(payload.task());
                }));
    }

    private static boolean isRemoteOpen(ServerPlayer player) {
        return player.getMainHandItem().getItem() instanceof fr.droneartisan.item.DroneRemoteItem
                || player.getOffhandItem().getItem() instanceof fr.droneartisan.item.DroneRemoteItem
                || player.containerMenu instanceof fr.droneartisan.menu.DroneControlMenu menu
                && menu.stillValid(player);
    }
}
