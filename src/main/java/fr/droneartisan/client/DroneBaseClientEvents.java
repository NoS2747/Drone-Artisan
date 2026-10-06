package fr.droneartisan.client;

import fr.droneartisan.DroneArtisan;
import fr.droneartisan.menu.DroneBaseMenuRegistry;
import fr.droneartisan.menu.DroneControlMenuRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = DroneArtisan.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DroneBaseClientEvents {
    private DroneBaseClientEvents() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(DroneBaseMenuRegistry.BASE_CONTROL.get(), DroneBaseScreen::new);
        event.register(DroneControlMenuRegistry.DRONE_CONTROL.get(), DroneControlScreen::new);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        DroneCameraController.registerKeys(event);
    }
}
