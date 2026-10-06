package fr.droneartisan.menu;

import fr.droneartisan.DroneArtisan;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DroneBaseMenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, DroneArtisan.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<DroneBaseMenu>> BASE_CONTROL =
            MENUS.register("base_control",
                    () -> IMenuTypeExtension.create((IContainerFactory<DroneBaseMenu>) DroneBaseMenu::new));

    private DroneBaseMenuRegistry() {}
}
