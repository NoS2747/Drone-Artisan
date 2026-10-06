package fr.droneartisan.client;

import fr.droneartisan.DroneArtisan;
import fr.droneartisan.entity.DroneEntity;
import fr.droneartisan.entity.DroneRegistry;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = DroneArtisan.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DroneRenderEvents {
    private DroneRenderEvents() {}

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DroneRegistry.DRONE.get(), DroneRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DroneModel.LAYER, DroneModel::createLayer);
    }

    private static final class DroneRenderer extends MobRenderer<DroneEntity, DroneModel> {
        private DroneRenderer(EntityRendererProvider.Context context) {
            super(context, new DroneModel(context.bakeLayer(DroneModel.LAYER)), 0.35F);
        }

        @Override
        public ResourceLocation getTextureLocation(DroneEntity drone) {
            return ResourceLocation.fromNamespaceAndPath(DroneArtisan.MOD_ID, "textures/entity/drone.png");
        }
    }
}
