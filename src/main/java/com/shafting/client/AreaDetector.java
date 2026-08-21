package com.shafting.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

/**
 * Standalone area detector. It intentionally does not depend on NoFrills.
 * It looks for "Mineshaft" in the visible sidebar scoreboard text.
 */
public final class AreaDetector {
    private AreaDetector() {}

    public static boolean isMineshaft() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return false;
        if (plain(objective.getDisplayName()).toLowerCase().contains("mineshaft")) return true;
        for (var entry : scoreboard.listPlayerScores(objective)) {
            String text = entry.ownerName();
            if (text != null && text.toLowerCase().contains("mineshaft")) return true;
            Component display = entry.displayName();
            if (display != null && plain(display).toLowerCase().contains("mineshaft")) return true;
        }
        return false;
    }

    private static String plain(Component component) {
        return component == null ? "" : component.getString();
    }
}
