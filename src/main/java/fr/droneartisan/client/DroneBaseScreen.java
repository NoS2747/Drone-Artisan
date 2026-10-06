package fr.droneartisan.client;

import fr.droneartisan.entity.DroneEntity;
import fr.droneartisan.entity.DroneTask;
import fr.droneartisan.menu.DroneBaseMenu;
import fr.droneartisan.network.DroneBaseCommandPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DroneBaseScreen extends AbstractContainerScreen<DroneBaseMenu> {
    public DroneBaseScreen(DroneBaseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 396;
        imageHeight = 238;
    }

    @Override
    protected void init() {
        super.init();
        int left = leftPos + 119;
        int right = leftPos + 253;
        addTaskButton(left, 120, DroneTask.FOLLOW);
        addTaskButton(right, 120, DroneTask.STAY);
        addTaskButton(left, 140, DroneTask.COLLECT);
        addTaskButton(right, 140, DroneTask.HARVEST);
        addTaskButton(left, 160, DroneTask.RETURN_BASE);
        addRenderableWidget(new DronePanelButton(right, topPos + 160, 128, 18,
                Component.translatable("gui.droneartisan.pilot"), button ->
                DroneCameraController.start(menu.drone(minecraft.player))));
    }

    private void addTaskButton(int x, int y, DroneTask task) {
        addRenderableWidget(new DronePanelButton(x, topPos + y, 128, 18,
                Component.translatable(task.translation() + ".short"), button ->
                PacketDistributor.sendToServer(new DroneBaseCommandPayload(menu.basePos(), task.ordinal()))));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        DronePanelStyle.drawWindow(graphics, x, y, imageWidth, imageHeight);
        DronePanelStyle.drawPanel(graphics, x + 8, y + 35, 96, 193);
        DronePanelStyle.drawPanel(graphics, x + 111, y + 35, 277, 62);
        DronePanelStyle.drawPanel(graphics, x + 111, y + 102, 277, 80);
        DronePanelStyle.drawPanel(graphics, x + 111, y + 183, 277, 27);
        graphics.fill(x + 105, y + 35, x + 107, y + 228, 0xFF16434A);
        DronePanelStyle.drawDronePortrait(graphics, x + 28, y + 57, 1);

        DroneEntity drone = menu.drone(minecraft.player);
        DronePanelStyle.drawStatBar(graphics, x + 18, y + 132, 76,
                drone == null ? 0.0F : drone.getBatteryPercent(), 0xFF9BFFEC);
        DronePanelStyle.drawStatBar(graphics, x + 18, y + 177, 76,
                drone == null ? 0.0F : drone.getHealthPercent(), 0xFFFF8275);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 14, 12, 0xFF102E36, false);
        DroneEntity drone = menu.drone(minecraft.player);
        graphics.drawString(font, Component.translatable(drone == null
                        ? "gui.droneartisan.base_offline" : "gui.droneartisan.base_online"),
                18, 42, drone == null ? 0xFF792B3C : 0xFF102E36, false);
        graphics.drawString(font, Component.translatable("gui.droneartisan.current_task"),
                127, 43, 0xFF102E36, false);
        graphics.drawString(font, drone == null
                        ? Component.translatable("gui.droneartisan.drone_offline")
                        : Component.translatable(drone.getTask().translation()),
                127, 59, drone == null ? 0xFFFFE089 : 0xFFF4FFFF, false);
        graphics.drawString(font, Component.translatable("gui.droneartisan.battery_label"),
                18, 105, 0xFF102E36, false);
        graphics.drawString(font, drone == null ? "--" : drone.getBattery() + " / 24000",
                18, 118, 0xFFF4FFFF, false);
        graphics.drawString(font, Component.translatable("gui.droneartisan.health_label"),
                18, 151, 0xFF102E36, false);
        graphics.drawString(font, drone == null
                        ? Component.translatable("gui.droneartisan.drone_offline")
                        : Component.translatable("gui.droneartisan.health_hearts",
                        Math.round(drone.getHealth() / 2.0F), 20),
                18, 164, 0xFFF4FFFF, false);
        graphics.drawString(font, Component.translatable("gui.droneartisan.select_task"),
                119, 106, 0xFF102E36, false);
        graphics.drawString(font, Component.translatable("gui.droneartisan.base_link"),
                127, 192, 0xFFF4FFFF, false);
    }
}
