package com.shafting.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

public final class CorpseFeature {
    private static final Set<Integer> OPENED = new HashSet<>();

    public enum CorpseType { LAPIS, TUNGSTEN, UMBER, VANGUARD, NONE }

    private CorpseFeature() {}

    public static void tick() {
        if (!ShaftingClient.config.corpseHighlight || !AreaDetector.isMineshaft()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ArmorStand stand && !stand.isInvisible()) {
                // Rendering is handled by the custom renderer; this pass only keeps stale IDs out.
                if (OPENED.contains(stand.getId()) && !ShaftingClient.config.hideOpened) OPENED.remove(stand.getId());
            }
        }
    }

    public static void clearOpened() { OPENED.clear(); }

    public static boolean isOpened(ArmorStand stand) { return OPENED.contains(stand.getId()); }

    public static void interact(Entity entity) {
        if (!ShaftingClient.config.corpseHighlight || !ShaftingClient.config.hideOpened || !AreaDetector.isMineshaft()) return;
        if (!(entity instanceof ArmorStand stand)) return;
        CorpseType type = type(stand);
        if (type != CorpseType.NONE && hasKey(type)) OPENED.add(stand.getId());
    }

    public static CorpseType type(ArmorStand stand) {
        ItemStack helmet = stand.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        if (helmet.isEmpty()) return CorpseType.NONE;
        String name = helmet.getHoverName().getString().replaceAll("§.", "").trim();
        return switch (name) {
            case "Lapis Armor Helmet" -> CorpseType.LAPIS;
            case "Mineral Helmet" -> CorpseType.TUNGSTEN;
            case "Yog Helmet" -> CorpseType.UMBER;
            case "Vanguard Helmet" -> CorpseType.VANGUARD;
            default -> CorpseType.NONE;
        };
    }

    private static boolean hasKey(CorpseType type) {
        String id = switch (type) {
            case TUNGSTEN -> "TUNGSTEN_KEY";
            case UMBER -> "UMBER_KEY";
            case VANGUARD -> "SKELETON_KEY";
            default -> null;
        };
        if (id == null) return true;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                String itemId = stack.getHoverName().getString();
                if (itemId.equalsIgnoreCase(id) || itemId.replace(" ", "_").equalsIgnoreCase(id)) return true;
            }
        }
        return false;
    }
}
