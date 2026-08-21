package com.shafting.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class WorldEspRenderer {

    private WorldEspRenderer() {
    }

    public static void register() {
        LevelRenderEvents.BEFORE_TRANSLUCENT_TERRAIN.register(
                WorldEspRenderer::render
        );
    }

    private static void render(
            net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext context
    ) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            return;
        }

        PoseStack pose = context.poseStack();
        MultiBufferSource.BufferSource buffers = context.bufferSource();

        /*
         * Minecraft 26.1.2 no longer exposes context.camera().
         * The camera position is available through the level render state.
         */
        Vec3 camera = context.levelState().cameraRenderState.pos;

        pose.pushPose();

        // Move world coordinates relative to the camera.
        pose.translate(-camera.x, -camera.y, -camera.z);

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        /*
         * Littlefoot ESP
         */
        if (ShaftingClient.config.littlefootEsp) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!isLittlefoot(entity)) {
                    continue;
                }

                AABB box = entity.getBoundingBox().inflate(0.08);

                drawBox(
                        pose,
                        lines,
                        box,
                        1.0f,
                        0.15f,
                        0.9f,
                        1.0f
                );
            }
        }

        /*
         * Corpse ESP
         */
        if (ShaftingClient.config.corpseHighlight
                && AreaDetector.isMineshaft()) {

            for (Entity entity : mc.level.entitiesForRendering()) {

                if (!(entity instanceof ArmorStand stand)
                        || stand.isInvisible()) {
                    continue;
                }

                if (ShaftingClient.config.hideOpened
                        && CorpseFeature.isOpened(stand)) {
                    continue;
                }

                CorpseFeature.CorpseType type = CorpseFeature.type(stand);

                int color = switch (type) {
                    case LAPIS -> ShaftingClient.config.lapisColor;
                    case TUNGSTEN -> ShaftingClient.config.mineralColor;
                    case UMBER -> ShaftingClient.config.yogColor;
                    case VANGUARD -> ShaftingClient.config.vanguardColor;
                    default -> 0;
                };

                if (type == CorpseFeature.CorpseType.NONE) {
                    continue;
                }

                AABB box = stand
                        .getBoundingBox()
                        .inflate(0.25, 0.0, 0.25);

                drawBox(
                        pose,
                        lines,
                        box,
                        red(color),
                        green(color),
                        blue(color),
                        1.0f
                );
            }
        }

        pose.popPose();
    }

    private static boolean isLittlefoot(Entity entity) {
        if (!(entity instanceof Player)) {
            return false;
        }

        String name = entity.getName()
                .getString()
                .trim();

        String custom = entity.getCustomName() == null
                ? ""
                : entity.getCustomName()
                        .getString()
                        .trim();

        String display = entity.getDisplayName()
                .getString()
                .trim();

        return name.equalsIgnoreCase("Littlefoot")
                || custom.equalsIgnoreCase("Littlefoot")
                || display.equalsIgnoreCase("Littlefoot");
    }

    /**
     * Draws an AABB without using the removed
     * LevelRenderer.renderLineBox() method.
     */
    private static void drawBox(
            PoseStack pose,
            VertexConsumer vc,
            AABB box,
            float r,
            float g,
            float b,
            float a
    ) {
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;

        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        /*
         * Bottom
         */
        line(pose, vc, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        line(pose, vc, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        line(pose, vc, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        line(pose, vc, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        /*
         * Top
         */
        line(pose, vc, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(pose, vc, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        line(pose, vc, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        line(pose, vc, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        /*
         * Vertical edges
         */
        line(pose, vc, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        line(pose, vc, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(pose, vc, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        line(pose, vc, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    private static void line(
            PoseStack pose,
            VertexConsumer vc,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float r,
            float g,
            float b,
            float a
    ) {
        PoseStack.Pose last = pose.last();

        vc.addVertex(
                last,
                x1,
                y1,
                z1
        ).setColor(
                r,
                g,
                b,
                a
        );

        vc.addVertex(
                last,
                x2,
                y2,
                z2
        ).setColor(
                r,
                g,
                b,
                a
        );
    }

    private static float red(int argb) {
        return ((argb >>> 24) & 255) / 255.0f;
    }

    private static float green(int argb) {
        return ((argb >>> 16) & 255) / 255.0f;
    }

    private static float blue(int argb) {
        return ((argb >>> 8) & 255) / 255.0f;
    }
}
```
