package fr.droneartisan.entity;

import fr.droneartisan.DroneArtisan;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class DroneRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, DroneArtisan.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<DroneEntity>> DRONE =
            ENTITIES.register("drone", () -> EntityType.Builder
                    .of(DroneEntity::new, MobCategory.CREATURE).sized(0.8F, 0.6F)
                    .clientTrackingRange(10).updateInterval(2).build("drone"));

    private DroneRegistry() {}
}
