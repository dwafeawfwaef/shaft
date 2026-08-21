package com.shafting.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ShaftingScreen extends Screen {
    private Button littlefoot;
    private Button corpse;
    private Button hideOpened;
    private Button lapis;
    private Button mineral;
    private Button yog;
    private Button vanguard;

    public ShaftingScreen() {
        super(Component.literal("Shafting"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 90;
        littlefoot = addRenderableWidget(Button.builder(toggle("Littlefoot ESP", ShaftingClient.config.littlefootEsp), b -> {
            ShaftingClient.config.littlefootEsp = !ShaftingClient.config.littlefootEsp;
            b.setMessage(toggle("Littlefoot ESP", ShaftingClient.config.littlefootEsp));
            ShaftingClient.save();
        }).bounds(cx - 100, y, 200, 20).build());

        corpse = addRenderableWidget(Button.builder(toggle("Corpse Highlight", ShaftingClient.config.corpseHighlight), b -> {
            ShaftingClient.config.corpseHighlight = !ShaftingClient.config.corpseHighlight;
            b.setMessage(toggle("Corpse Highlight", ShaftingClient.config.corpseHighlight));
            ShaftingClient.save();
        }).bounds(cx - 100, y + 28, 200, 20).build());

        hideOpened = addRenderableWidget(Button.builder(toggle("Hide Opened", ShaftingClient.config.hideOpened), b -> {
            ShaftingClient.config.hideOpened = !ShaftingClient.config.hideOpened;
            b.setMessage(toggle("Hide Opened", ShaftingClient.config.hideOpened));
            ShaftingClient.save();
        }).bounds(cx - 100, y + 56, 200, 20).build());

        lapis = colorButton(cx, y + 92, "Lapis", 0);
        mineral = colorButton(cx, y + 118, "Mineral", 1);
        yog = colorButton(cx, y + 144, "Yog", 2);
        vanguard = colorButton(cx, y + 170, "Vanguard", 3);
    }

    private Button colorButton(int cx, int y, String label, int type) {
        return addRenderableWidget(Button.builder(Component.literal(label + ": " + hex(color(type))), b -> {
            cycle(type);
            b.setMessage(Component.literal(label + ": " + hex(color(type))));
            ShaftingClient.save();
        }).bounds(cx - 100, y, 200, 20).build());
    }

    private void cycle(int type) {
        int[] colors = {0x5555FFFF, 0xAAAAAAFF, 0xFFAA00FF, 0xFF55FFFF, 0xFF55FFFF, 0x55FFFFFF, 0x55FF55FF, 0xFF5555FF, 0xFFFFFFFF};
        int current = color(type);
        int next = colors[0];
        for (int i = 0; i < colors.length; i++) if (colors[i] == current) { next = colors[(i + 1) % colors.length]; break; }
        setColor(type, next);
    }

    private int color(int type) {
        return switch (type) {
            case 0 -> ShaftingClient.config.lapisColor;
            case 1 -> ShaftingClient.config.mineralColor;
            case 2 -> ShaftingClient.config.yogColor;
            default -> ShaftingClient.config.vanguardColor;
        };
    }

    private void setColor(int type, int value) {
        switch (type) {
            case 0 -> ShaftingClient.config.lapisColor = value;
            case 1 -> ShaftingClient.config.mineralColor = value;
            case 2 -> ShaftingClient.config.yogColor = value;
            default -> ShaftingClient.config.vanguardColor = value;
        }
    }

    private static Component toggle(String name, boolean value) {
        return Component.literal(name + ": " + (value ? "ON" : "OFF"));
    }

    private static String hex(int color) { return String.format("#%08X", color); }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 120, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.literal("/shafting"), this.width / 2, this.height - 25, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        ShaftingClient.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
