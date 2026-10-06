package fr.droneartisan.client;

import net.minecraft.client.gui.GuiGraphics;

public final class DronePanelStyle {
    private DronePanelStyle() {}

    public static void drawWindow(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF101F24);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF087681);
        graphics.fill(x + 5, y + 5, x + width - 5, y + height - 5, 0xFF00B2BE);
        graphics.fill(x + 6, y + 6, x + width - 6, y + 28, 0xFF00A6B2);
        graphics.fill(x + width - 15, y + 10, x + width - 9, y + 16, 0xFF132E35);
        graphics.fill(x + width - 13, y + 12, x + width - 11, y + 14, 0xFF91F4EB);
    }

    public static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF12343B);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF008F9D);
        graphics.fill(x + 4, y + 4, x + width - 4, y + 5, 0xFF25CBD0);
    }

    public static void drawStatBar(GuiGraphics graphics, int x, int y, int width, float ratio, int fillColor) {
        int clampedWidth = Math.round((width - 2) * Math.max(0.0F, Math.min(1.0F, ratio)));
        graphics.fill(x, y, x + width, y + 6, 0xFF12343B);
        if (clampedWidth > 0) graphics.fill(x + 1, y + 1, x + 1 + clampedWidth, y + 5, fillColor);
    }

    public static void drawDronePortrait(GuiGraphics graphics, int x, int y, int scale) {
        int dark = 0xFF12333A;
        int metal = 0xFFB8D2CE;
        int cyan = 0xFF11E5E0;
        int coreX = x + 21 * scale;
        int coreY = y + 17 * scale;
        graphics.fill(x + 8 * scale, y + 8 * scale, x + 15 * scale, y + 12 * scale, dark);
        graphics.fill(x + 45 * scale, y + 8 * scale, x + 52 * scale, y + 12 * scale, dark);
        graphics.fill(x + 10 * scale, y + 5 * scale, x + 14 * scale, y + 9 * scale, metal);
        graphics.fill(x + 46 * scale, y + 5 * scale, x + 50 * scale, y + 9 * scale, metal);
        graphics.fill(x + 12 * scale, y + 11 * scale, x + 22 * scale, y + 16 * scale, dark);
        graphics.fill(x + 38 * scale, y + 11 * scale, x + 48 * scale, y + 16 * scale, dark);
        graphics.fill(coreX - 5 * scale, coreY - 6 * scale,
                coreX + 5 * scale, coreY + 5 * scale, dark);
        graphics.fill(coreX - 3 * scale, coreY - 4 * scale,
                coreX + 3 * scale, coreY + 2 * scale, metal);
        graphics.fill(coreX - 2 * scale, coreY - 3 * scale,
                coreX + 2 * scale, coreY + 1 * scale, cyan);
        graphics.fill(coreX - 2 * scale, coreY + 5 * scale,
                coreX + 2 * scale, coreY + 11 * scale, dark);
        graphics.fill(coreX - 7 * scale, coreY + 10 * scale,
                coreX + 7 * scale, coreY + 13 * scale, metal);
        graphics.fill(x + 15 * scale, y + 27 * scale, x + 19 * scale, y + 34 * scale, dark);
        graphics.fill(x + 31 * scale, y + 27 * scale, x + 35 * scale, y + 34 * scale, dark);
        graphics.fill(x + 11 * scale, y + 33 * scale, x + 21 * scale, y + 36 * scale, metal);
        graphics.fill(x + 29 * scale, y + 33 * scale, x + 39 * scale, y + 36 * scale, metal);
    }
}
