package fr.droneartisan.client;

import fr.droneartisan.entity.DroneEntity;
import fr.droneartisan.entity.DroneTask;
import fr.droneartisan.menu.DroneControlMenu;
import fr.droneartisan.network.DroneControlCommandPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DroneControlScreen extends AbstractContainerScreen<DroneControlMenu> {
    public DroneControlScreen(DroneControlMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 396;
        imageHeight = 238;
        inventoryLabelY = 138;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new DronePanelButton(leftPos + 244, topPos + 8, 142, 18,
                Component.translatable("gui.droneartisan.pilot"), button ->
                        DroneCameraController.start(menu.drone())));
        int left = leftPos + 178;
        int right = leftPos + 284;
        addTaskButton(left, 50, DroneTask.FOLLOW);
        addTaskButton(right, 50, DroneTask.STAY);
        addTaskButton(left, 69, DroneTask.COLLECT);
        addTaskButton(right, 69, DroneTask.HARVEST);
        addTaskButton(left, 88, DroneTask.MINE);
        addTaskButton(right, 88, DroneTask.LOCATE_ORE);
        addTaskButton(left, 107, DroneTask.DEFEND);
        addTaskButton(right, 107, DroneTask.RETURN_BASE);
    }

    private void addTaskButton(int x, int y, DroneTask task) {
        addRenderableWidget(new DronePanelButton(x, topPos + y, 102, 17,
                Component.translatable(task.translation() + ".short"), button ->
                PacketDistributor.sendToServer(new DroneControlCommandPayload(menu.drone() == null
                        ? -1 : menu.drone().getId(), task.ordinal()))));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        DronePanelStyle.drawWindow(graphics, x, y, imageWidth, imageHeight);
        DronePanelStyle.drawPanel(graphics, x + 8, y + 35, 162, 67);
        DronePanelStyle.drawPanel(graphics, x + 175, y + 35, 213, 104);
        DronePanelStyle.drawPanel(graphics, x + 7, y + 108, 163, 122);
        DronePanelStyle.drawPanel(graphics, x + 175, y + 108, 213, 122);
        graphics.fill(x + 174, y + 35, x + 176, y + 139, 0xFF16434A);

        DroneEntity drone = menu.drone();
        DronePanelStyle.drawDronePortrait(graphics, x + 108, y + 40, 1);
        if (drone != null) {
            DronePanelStyle.drawStatBar(graphics, x + 17, y + 66, 81, drone.getBatteryPercent(),
                    drone.getBatteryPercent() > 0.2F ? 0xFF9BFFEC : 0xFFFFBC63);
            DronePanelStyle.drawStatBar(graphics, x + 17, y + 96, 81, drone.getHealthPercent(), 0xFFFF8275);
        } else {
            DronePanelStyle.drawStatBar(graphics, x + 17, y + 66, 81, 0.0F, 0xFF9BFFEC);
            DronePanelStyle.drawStatBar(graphics, x + 17, y + 96, 81, 0.0F, 0xFFFF8275);
        }
        drawCargoSlots(graphics, x, y);
    }

    private static void drawCargoSlots(GuiGraphics graphics, int x, int y) {
        for (int slot = 0; slot < 9; slot++) {
            drawSlot(graphics, x + 7 + slot * 18, y + 132);
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, x + 7 + column * 18, y + 154 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, x + 7 + column * 18, y + 212);
        }
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF104D55);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF008895);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 13, 12, 0xFF102E36, false);
        DroneEntity drone = menu.drone();
        if (drone == null) {
            graphics.drawString(font, Component.translatable("gui.droneartisan.drone_offline"),
                    16, 53, 0xFFFFE089, false);
        } else {
            graphics.drawString(font, Component.translatable("gui.droneartisan.battery_label"),
                    16, 43, 0xFF103A40, false);
            graphics.drawString(font, Component.literal(drone.getBattery() + " / 24000"),
                    16, 54, 0xFFEDFFFF, false);
            graphics.drawString(font, Component.translatable("gui.droneartisan.health_label"),
                    16, 75, 0xFF103A40, false);
            graphics.drawString(font, Component.translatable("gui.droneartisan.health_hearts",
                    Math.round(drone.getHealth() / 2.0F), 20), 16, 86, 0xFFEDFFFF, false);
        }
        graphics.drawString(font, Component.translatable("gui.droneartisan.instruction"),
                180, 39, 0xFF102E36, false);
        graphics.drawString(font, Component.translatable("gui.droneartisan.cargo"),
                12, 112, 0xFF102E36, false);
        graphics.drawString(font, Component.translatable("gui.droneartisan.controls_move"),
                184, 140, 0xFFEDFFFF, false);
        graphics.drawString(font, Component.translatable("gui.droneartisan.controls_actions"),
                184, 158, 0xFFEDFFFF, false);
        graphics.drawString(font, Component.translatable("gui.droneartisan.controls_laser"),
                184, 176, 0xFFEDFFFF, false);
        if (drone != null) {
            graphics.drawString(font, Component.translatable(drone.getTask().translation() + ".short"),
                    166, 12, 0xFF9C2D3C, false);
        }
    }
}
