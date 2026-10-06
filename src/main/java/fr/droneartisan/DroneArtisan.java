package fr.droneartisan;

import fr.droneartisan.entity.DroneEntity;
import fr.droneartisan.entity.DroneRegistry;
import fr.droneartisan.item.DroneItems;
import fr.droneartisan.block.DroneBaseRegistry;
import fr.droneartisan.network.DroneBaseNetwork;
import fr.droneartisan.menu.DroneBaseMenuRegistry;
import fr.droneartisan.menu.DroneControlMenuRegistry;
import fr.droneartisan.network.DroneControlNetwork;
import fr.droneartisan.network.DronePilotNetwork;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(DroneArtisan.MOD_ID)
public final class DroneArtisan {
    public static final String MOD_ID = "droneartisan";
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final net.neoforged.neoforge.registries.DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            TABS.register("items", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.droneartisan"))
                    .icon(() -> new net.minecraft.world.item.ItemStack(DroneItems.DRONE_KIT.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(DroneItems.DRONE_KIT.get());
                        output.accept(DroneItems.REMOTE.get());
                        output.accept(DroneItems.DRONE_BASE.get());
                    }).build());

    public DroneArtisan(IEventBus modBus) {
        DroneRegistry.ENTITIES.register(modBus);
        DroneItems.ITEMS.register(modBus);
        DroneBaseRegistry.BLOCKS.register(modBus);
        DroneBaseRegistry.BLOCK_ENTITIES.register(modBus);
        DroneBaseMenuRegistry.MENUS.register(modBus);
        DroneControlMenuRegistry.MENUS.register(modBus);
        TABS.register(modBus);
        modBus.addListener(DroneEntity::registerAttributes);
        modBus.addListener(DroneBaseNetwork::register);
        modBus.addListener(DroneControlNetwork::register);
        modBus.addListener(DronePilotNetwork::register);
    }
}
