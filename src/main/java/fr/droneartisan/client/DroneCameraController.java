package fr.droneartisan.client;

import com.mojang.blaze3d.platform.InputConstants;
import fr.droneartisan.DroneArtisan;
import fr.droneartisan.entity.DroneEntity;
import fr.droneartisan.network.DronePilotPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = DroneArtisan.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class DroneCameraController {
    private static final KeyMapping EXIT_CAMERA = new KeyMapping(
            "key.droneartisan.camera_exit", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V,
            "key.categories.droneartisan");
    private static final KeyMapping FIRE_LASER = new KeyMapping(
            "key.droneartisan.fire_laser", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G,
            "key.categories.droneartisan");
    private static int cameraDroneId = -1;
    private static Vec3 playerAnchor;

    private DroneCameraController() {}

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(EXIT_CAMERA);
        event.register(FIRE_LASER);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (cameraDroneId < 0) return;
        if (EXIT_CAMERA.consumeClick()) {
            stop(minecraft, true);
            return;
        }
        if (minecraft.level == null || minecraft.player == null
                || !(minecraft.level.getEntity(cameraDroneId) instanceof DroneEntity drone)
                || !drone.isAlive() || !drone.isOwnedBy(minecraft.player)) {
            stop(minecraft, false);
            return;
        }
        drone.setYRot(minecraft.player.getYRot());
        drone.setXRot(minecraft.player.getXRot());
        if (playerAnchor != null) {
            minecraft.player.setPos(playerAnchor.x, playerAnchor.y, playerAnchor.z);
            minecraft.player.setDeltaMovement(Vec3.ZERO);
            minecraft.player.fallDistance = 0.0F;
        }
        int action = FIRE_LASER.consumeClick() ? DronePilotPayload.LASER
                : minecraft.screen == null && minecraft.hitResult instanceof BlockHitResult
                && org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS
                ? DronePilotPayload.MINE : DronePilotPayload.NONE;
        sendInput(minecraft, drone, action, minecraft.hitResult, minecraft.screen != null);
    }

    @SubscribeEvent
    public static void mouseButton(InputEvent.MouseButton.Pre event) {
        if (cameraDroneId < 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null) return;
        if (event.getAction() != GLFW.GLFW_PRESS) {
            event.setCanceled(true);
            return;
        }
        int action = DronePilotPayload.NONE;
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (minecraft.hitResult instanceof BlockHitResult) {
                action = DronePilotPayload.MINE;
            }
        } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (minecraft.hitResult instanceof EntityHitResult) {
                action = DronePilotPayload.INTERACT_ENTITY;
            } else if (minecraft.hitResult instanceof BlockHitResult) {
                action = DronePilotPayload.INTERACT_BLOCK;
            }
        }
        if (action != DronePilotPayload.NONE) {
            DroneEntity drone = currentDrone(minecraft);
            if (drone != null) sendInput(minecraft, drone, action, minecraft.hitResult);
        }
        event.setCanceled(true);
    }

    public static void start(DroneEntity drone) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || drone == null
                || !drone.isAlive() || drone.level() != minecraft.level
                || !drone.isOwnedBy(minecraft.player)) return;
        cameraDroneId = drone.getId();
        playerAnchor = minecraft.player.position();
        sendInput(minecraft, drone, DronePilotPayload.START_PILOT, null);
        minecraft.setCameraEntity(drone);
        minecraft.setScreen(null);
        minecraft.player.displayClientMessage(Component.translatable("message.droneartisan.pilot_started"), true);
    }

    private static void stop(Minecraft minecraft, boolean showMessage) {
        DroneEntity drone = currentDrone(minecraft);
        if (drone != null) sendInput(minecraft, drone, DronePilotPayload.STOP_PILOT, null, true);
        cameraDroneId = -1;
        if (minecraft.player != null) {
            if (playerAnchor != null) {
                minecraft.player.setPos(playerAnchor.x, playerAnchor.y, playerAnchor.z);
                minecraft.player.setDeltaMovement(Vec3.ZERO);
                minecraft.player.fallDistance = 0.0F;
            }
            minecraft.setCameraEntity(minecraft.player);
            if (showMessage) {
                minecraft.player.displayClientMessage(
                        Component.translatable("message.droneartisan.pilot_stopped"), true);
            }
        }
        playerAnchor = null;
    }

    private static DroneEntity currentDrone(Minecraft minecraft) {
        return minecraft.level != null && minecraft.level.getEntity(cameraDroneId) instanceof DroneEntity drone
                ? drone : null;
    }

    private static void sendInput(Minecraft minecraft, DroneEntity drone, int action,
                                  net.minecraft.world.phys.HitResult hit) {
        sendInput(minecraft, drone, action, hit, false);
    }

    private static void sendInput(Minecraft minecraft, DroneEntity drone, int action,
                                  net.minecraft.world.phys.HitResult hit, boolean stop) {
        if (minecraft.player == null) return;
        float forward = stop ? 0.0F
                : (minecraft.options.keyUp.isDown() ? 1.0F : 0.0F)
                - (minecraft.options.keyDown.isDown() ? 1.0F : 0.0F);
        float strafe = stop ? 0.0F
                : (minecraft.options.keyRight.isDown() ? 1.0F : 0.0F)
                - (minecraft.options.keyLeft.isDown() ? 1.0F : 0.0F);
        float vertical = stop ? 0.0F
                : (minecraft.options.keyJump.isDown() ? 1.0F : 0.0F)
                - (minecraft.options.keyShift.isDown() ? 1.0F : 0.0F);
        BlockPos pos = BlockPos.ZERO;
        int face = Direction.UP.get3DDataValue();
        int entityId = -1;
        if (hit instanceof BlockHitResult blockHit
                && blockHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            pos = blockHit.getBlockPos();
            face = blockHit.getDirection().get3DDataValue();
        } else if (hit instanceof EntityHitResult entityHit) {
            entityId = entityHit.getEntity().getId();
        }
        PacketDistributor.sendToServer(new DronePilotPayload(drone.getId(), forward, strafe, vertical,
                minecraft.player.getYRot(), minecraft.player.getXRot(),
                !stop && minecraft.options.keySprint.isDown(), action, pos, face, entityId));
    }
}
