package com.shafting.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ShaftingScreen extends Screen {

    private Button littlefoot;
    private Button corpse;
    private Button hideOpened;

    public ShaftingScreen() {
        super(Component.literal("Shafting"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 60;

        littlefoot = this.addRenderableWidget(
                Button.builder(
                        toggle("Littlefoot ESP", ShaftingClient.config.littlefootEsp),
                        button -> {
                            ShaftingClient.config.littlefootEsp =
                                    !ShaftingClient.config.littlefootEsp;
                            button.setMessage(
                                    toggle("Littlefoot ESP",
                                            ShaftingClient.config.littlefootEsp)
                            );
                        }
                ).bounds(cx - 100, y, 200, 20).build()
        );

        y += 25;

        corpse = this.addRenderableWidget(
                Button.builder(
                        toggle("Corpse Highlight", ShaftingClient.config.corpseHighlight),
                        button -> {
                            ShaftingClient.config.corpseHighlight =
                                    !ShaftingClient.config.corpseHighlight;
                            button.setMessage(
                                    toggle("Corpse Highlight",
                                            ShaftingClient.config.corpseHighlight)
                            );
                        }
                ).bounds(cx - 100, y, 200, 20).build()
        );

        y += 25;

        hideOpened = this.addRenderableWidget(
                Button.builder(
                        toggle("Hide Opened", ShaftingClient.config.hideOpened),
                        button -> {
                            ShaftingClient.config.hideOpened =
                                    !ShaftingClient.config.hideOpened;
                            button.setMessage(
                                    toggle("Hide Opened",
                                            ShaftingClient.config.hideOpened)
                            );
                        }
                ).bounds(cx - 100, y, 200, 20).build()
        );
    }

    private static Component toggle(String name, boolean enabled) {
        return Component.literal(
                name + ": " + (enabled ? "ON" : "OFF")
        );
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.text(
                this.font,
                "Shafting",
                this.width / 2 - this.font.width("Shafting") / 2,
                this.height / 2 - 95,
                0xFFFFFFFF,
                true
        );

        String commandText = "/shafting";

        graphics.text(
                this.font,
                commandText,
                this.width / 2 - this.font.width(commandText) / 2,
                this.height / 2 + 70,
                0xFFAAAAAA,
                false
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
