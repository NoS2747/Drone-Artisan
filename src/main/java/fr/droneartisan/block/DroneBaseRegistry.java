package fr.droneartisan.block;

import fr.droneartisan.DroneArtisan;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DroneBaseRegistry {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, DroneArtisan.MOD_ID);
    public static final DeferredHolder<Block, DroneBaseBlock> DRONE_BASE =
            BLOCKS.register("drone_base", () -> new DroneBaseBlock(
                    Block.Properties.of().strength(4.0F).requiresCorrectToolForDrops()));
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DroneArtisan.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DroneBaseBlockEntity>> DRONE_BASE_ENTITY =
            BLOCK_ENTITIES.register("drone_base",
                    () -> BlockEntityType.Builder.of(DroneBaseBlockEntity::new, DRONE_BASE.get()).build(null));

    private DroneBaseRegistry() {}
}
