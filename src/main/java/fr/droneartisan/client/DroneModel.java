package fr.droneartisan.client;

import fr.droneartisan.DroneArtisan;
import fr.droneartisan.entity.DroneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public final class DroneModel extends EntityModel<DroneEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(DroneArtisan.MOD_ID, "drone"), "main");
    private final ModelPart body;
    private final ModelPart rotorLeft;
    private final ModelPart rotorRight;
    private final ModelPart rotorBack;

    public DroneModel(ModelPart root) {
        body = root.getChild("body");
        rotorLeft = root.getChild("rotor_left");
        rotorRight = root.getChild("rotor_right");
        rotorBack = root.getChild("rotor_back");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -2.0F, -4.0F, 8.0F, 4.0F, 8.0F)
                        .texOffs(0, 13).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 1.0F, 4.0F)
                        .texOffs(32, 0).addBox(-2.0F, -1.0F, -4.5F, 4.0F, 2.0F, 0.5F),
                PartPose.ZERO);
        CubeListBuilder rotor = CubeListBuilder.create()
                .texOffs(32, 8).addBox(-4.0F, -0.5F, -0.5F, 8.0F, 1.0F, 1.0F)
                .texOffs(32, 12).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 8.0F, 1.0F);
        root.addOrReplaceChild("rotor_left", rotor, PartPose.offset(-5.0F, -2.5F, 0.0F));
        root.addOrReplaceChild("rotor_right", rotor, PartPose.offset(5.0F, -2.5F, 0.0F));
        root.addOrReplaceChild("rotor_back", rotor, PartPose.offset(0.0F, -2.5F, 5.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(DroneEntity drone, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float spin = ageInTicks * 1.3F;
        rotorLeft.zRot = spin;
        rotorRight.zRot = -spin;
        rotorBack.zRot = spin;
        body.y = (float) Math.sin(ageInTicks * 0.12F) * 0.35F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        body.render(poseStack, buffer, packedLight, packedOverlay, color);
        rotorLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        rotorRight.render(poseStack, buffer, packedLight, packedOverlay, color);
        rotorBack.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
