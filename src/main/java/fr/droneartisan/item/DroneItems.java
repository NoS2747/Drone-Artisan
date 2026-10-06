package fr.droneartisan.item;

import fr.droneartisan.DroneArtisan;
import fr.droneartisan.block.DroneBaseRegistry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class DroneItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DroneArtisan.MOD_ID);
    public static final DeferredHolder<Item, Item> DRONE_KIT =
            ITEMS.register("drone_kit", () -> new DroneKitItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, DroneRemoteItem> REMOTE =
            ITEMS.register("remote", () -> new DroneRemoteItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, BlockItem> DRONE_BASE =
            ITEMS.register("drone_base", () -> new BlockItem(DroneBaseRegistry.DRONE_BASE.get(),
                    new Item.Properties()));

    private DroneItems() {}
}
