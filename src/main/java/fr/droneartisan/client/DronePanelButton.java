package fr.droneartisan.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class DronePanelButton extends Button {
    public DronePanelButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean highlighted = isHoveredOrFocused();
        int border = highlighted ? 0xFF102E36 : 0xFF16434A;
        int face = !active ? 0xFF418A8F : highlighted ? 0xFF00A6B2 : 0xFF008F9D;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, border);
        graphics.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, face);
        graphics.fill(getX() + 3, getY() + 3, getX() + width - 3, getY() + 4,
                highlighted ? 0xFF71F2EA : 0xFF36D8D3);
        graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                getX() + width / 2, getY() + (height - 8) / 2, active ? 0xFFE7FFFF : 0xFFB3C9C8);
    }
}
