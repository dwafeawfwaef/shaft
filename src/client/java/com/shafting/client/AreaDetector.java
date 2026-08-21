package com.shafting.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

/** Client-side detector for the visible Mineshaft scoreboard area. */
public final class AreaDetector {
    private AreaDetector() {}

    public static boolean isMineshaft() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;

        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return false;

        if (containsMineshaft(objective.getDisplayName())) return true;
        for (var entry : scoreboard.listPlayerScores(objective)) {
            if (containsMineshaft(entry.ownerName())) return true;
        }
        return false;
    }

    private static boolean containsMineshaft(Component component) {
        return component != null && component.getString().toLowerCase(java.util.Locale.ROOT).contains("mineshaft");
    }
}
