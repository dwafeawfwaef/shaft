package com.shafting.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class WorldEspRenderer {
    private WorldEspRenderer() {}

    public static void register() {
        LevelRenderEvents.AFTER_ENTITIES.register(WorldEspRenderer::render);
    }

    private static void render(LevelRenderEvents.AfterEntities context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        PoseStack pose = context.poseStack();
        MultiBufferSource.BufferSource buffers = context.bufferSource();
        Vec3 camera = context.camera().getPosition();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);

        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        if (ShaftingClient.config.littlefootEsp) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (isLittlefoot(entity)) {
                    AABB box = entity.getBoundingBox().inflate(0.08);
                    drawBox(pose, lines, box, 1f, 0.15f, 0.9f, 1f);
                }
            }
        }

        if (ShaftingClient.config.corpseHighlight && AreaDetector.isMineshaft()) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof ArmorStand stand) || stand.isInvisible()) continue;
                if (ShaftingClient.config.hideOpened && CorpseFeature.isOpened(stand)) continue;
                CorpseFeature.CorpseType type = CorpseFeature.type(stand);
                int color = switch (type) {
                    case LAPIS -> ShaftingClient.config.lapisColor;
                    case TUNGSTEN -> ShaftingClient.config.mineralColor;
                    case UMBER -> ShaftingClient.config.yogColor;
                    case VANGUARD -> ShaftingClient.config.vanguardColor;
                    default -> 0;
                };
                if (type != CorpseFeature.CorpseType.NONE) {
                    AABB box = stand.getBoundingBox().inflate(0.25, 0.0, 0.25);
                    drawBox(pose, lines, box, red(color), green(color), blue(color), 1f);
                }
            }
        }

        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        pose.popPose();
    }

    private static boolean isLittlefoot(Entity entity) {
        if (!(entity instanceof Player)) return false;
        String name = entity.getName().getString().trim();
        String custom = entity.getCustomName() == null ? "" : entity.getCustomName().getString().trim();
        String display = entity.getDisplayName().getString().trim();
        return name.equalsIgnoreCase("Littlefoot") || custom.equalsIgnoreCase("Littlefoot") || display.equalsIgnoreCase("Littlefoot");
    }

    private static void drawBox(PoseStack pose, VertexConsumer vc, AABB box, float r, float g, float b, float a) {
        LevelRenderer.renderLineBox(pose, vc, box, r, g, b, a);
    }

    private static float red(int argb) { return ((argb >>> 24) & 255) / 255f; }
    private static float green(int argb) { return ((argb >>> 16) & 255) / 255f; }
    private static float blue(int argb) { return ((argb >>> 8) & 255) / 255f; }
}
