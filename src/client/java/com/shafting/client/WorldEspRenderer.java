```java
package com.shafting.client;

import java.util.Optional;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.joml.Matrix4fc;

import org.lwjgl.system.MemoryUtil;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;

import net.minecraft.resources.Identifier;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class WorldEspRenderer {

    private static final RenderPipeline ESP_LINES = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_LINES_SNIPPET)
                    .withLocation(
                            Identifier.fromNamespaceAndPath(
                                    "shafting",
                                    "pipeline/esp_lines"
                            )
                    )
                    .withDepthStencilState(Optional.empty())
                    .build()
    );

    private static final ByteBufferBuilder ALLOCATOR =
            new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);

    private static BufferBuilder buffer;

    private static MappableRingBuffer vertexBuffer;

    private WorldEspRenderer() {
    }

    public static void register() {
        LevelRenderEvents.END_EXTRACTION.register(
                WorldEspRenderer::extract
        );

        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(
                WorldEspRenderer::render
        );
    }

    private static void extract(LevelExtractionContext context) {
        /*
         * Entity/world data is intentionally collected during extraction.
         * The actual GPU drawing happens during the drawing phase.
         */
    }

    private static void render(LevelRenderContext context) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            return;
        }

        PoseStack pose = context.poseStack();

        Vec3 camera = context.levelState()
                .cameraRenderState
                .pos;

        pose.pushPose();

        pose.translate(
                -camera.x,
                -camera.y,
                -camera.z
        );

        if (buffer == null) {
            buffer = new BufferBuilder(
                    ALLOCATOR,
                    ESP_LINES.getVertexFormatMode(),
                    ESP_LINES.getVertexFormat()
            );
        }

        boolean renderedAnything = false;

        if (ShaftingClient.config.littlefootEsp) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!isLittlefoot(entity)) {
                    continue;
                }

                AABB box = entity
                        .getBoundingBox()
                        .inflate(0.08);

                drawBox(
                        pose.last().pose(),
                        buffer,
                        box,
                        1.0f,
                        0.15f,
                        0.9f,
                        1.0f
                );

                renderedAnything = true;
            }
        }

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

                CorpseFeature.CorpseType type =
                        CorpseFeature.type(stand);

                int color = switch (type) {
                    case LAPIS ->
                            ShaftingClient.config.lapisColor;

                    case TUNGSTEN ->
                            ShaftingClient.config.mineralColor;

                    case UMBER ->
                            ShaftingClient.config.yogColor;

                    case VANGUARD ->
                            ShaftingClient.config.vanguardColor;

                    default ->
                            0;
                };

                if (type == CorpseFeature.CorpseType.NONE) {
                    continue;
                }

                AABB box = stand
                        .getBoundingBox()
                        .inflate(0.25, 0.0, 0.25);

                drawBox(
                        pose.last().pose(),
                        buffer,
                        box,
                        red(color),
                        green(color),
                        blue(color),
                        1.0f
                );

                renderedAnything = true;
            }
        }

        pose.popPose();

        if (!renderedAnything) {
            return;
        }

        drawBuffer(mc);
    }

    private static boolean isLittlefoot(Entity entity) {
        if (!(entity instanceof Player)) {
            return false;
        }

        String name = entity
                .getName()
                .getString()
                .trim();

        String custom = entity.getCustomName() == null
                ? ""
                : entity.getCustomName()
                        .getString()
                        .trim();

        String display = entity
                .getDisplayName()
                .getString()
                .trim();

        return name.equalsIgnoreCase("Littlefoot")
                || custom.equalsIgnoreCase("Littlefoot")
                || display.equalsIgnoreCase("Littlefoot");
    }

    private static void drawBox(
            Matrix4fc matrix,
            BufferBuilder builder,
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

        line(
                matrix,
                builder,
                minX, minY, minZ,
                maxX, minY, minZ,
                r, g, b, a
        );

        line(
                matrix,
                builder,
                maxX, minY, minZ,
                maxX, minY, maxZ,
                r, g, b, a
        );

        line(
                matrix,
                builder,
                maxX, minY, maxZ,
                minX, minY, maxZ,
                r, g, b, a
        );

        line(
                matrix,
                builder,
                minX, minY, maxZ,
                minX, minY, minZ,
                r, g, b, a
        );

        line(
                matrix,
                builder,
                minX, maxY, minZ,
                maxX, maxY, minZ,
                r, g, b, a
        );

        line(
                matrix,
                builder,
                maxX, maxY, minZ,
                maxX, maxY, maxZ,
                r, g, b, a
        );

        line(
                matrix,
                builder,
                maxX, maxY, maxZ,
                minX, maxY, maxZ,
                r, g, b, a
        );

        line(
                matrix,
                builder,
                minX, maxY, maxZ,
                minX, maxY, minZ,
                r, g, b, a
        );

        line(
                matrix,
                builder,
                minX, minY, minZ,
                minX, maxY, minZ,
                r, g, b, a
        );

        line(
                matrix,
                builder,
                maxX, minY, minZ,
                maxX, maxY, minZ,
                r, g, b, a
        );

        line(
                matrix,
                builder,
                maxX, minY, maxZ,
                maxX, maxY, maxZ,
                r, g, b, a
        );

        line(
                matrix,
                builder,
                minX, minY, maxZ,
                minX, maxY, maxZ,
                r, g, b, a
        );
    }

    private static void line(
            Matrix4fc matrix,
            BufferBuilder builder,
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
        builder.addVertex(
                matrix,
                x1,
                y1,
                z1
        ).setColor(
                r,
                g,
                b,
                a
        );

        builder.addVertex(
                matrix,
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

    private static void drawBuffer(Minecraft client) {
        if (buffer == null) {
            return;
        }

        MeshData mesh = buffer.buildOrThrow();

        if (mesh.vertexBuffer() == null) {
            mesh.close();
            buffer = null;
            return;
        }

        MeshData.DrawState drawState = mesh.drawState();

        VertexFormat format = drawState.format();

        int vertexBufferSize =
                drawState.vertexCount()
                        * format.getVertexSize();

        if (vertexBuffer == null
                || vertexBuffer.size() < vertexBufferSize) {

            if (vertexBuffer != null) {
                vertexBuffer.close();
            }

            vertexBuffer = new MappableRingBuffer(
                    () -> "shafting_esp",
                    GpuBuffer.USAGE_VERTEX
                            | GpuBuffer.USAGE_MAP_WRITE,
                    vertexBufferSize
            );
        }

        GpuBuffer gpuBuffer = vertexBuffer.currentBuffer();

        CommandEncoder encoder =
                RenderSystem.getDevice()
                        .createCommandEncoder();

        try (
                GpuBuffer.MappedView mapped =
                        encoder.mapBuffer(
                                gpuBuffer.slice(
                                        0,
                                        mesh.vertexBuffer().remaining()
                                ),
                                false,
                                true
                        )
        ) {
            MemoryUtil.memCopy(
                    mesh.vertexBuffer(),
                    mapped.data()
            );
        }

        draw(
                client,
                mesh,
                drawState,
                gpuBuffer,
                format
        );

        mesh.close();

        vertexBuffer.rotate();

        buffer = null;
    }

    private static void draw(
            Minecraft client,
            MeshData mesh,
            MeshData.DrawState drawState,
            GpuBuffer vertices,
            VertexFormat format
    ) {
        GpuBuffer indexBuffer = null;
        VertexFormat.IndexType indexType = null;

        if (drawState.indexCount() > 0) {
            indexType = drawState.indexType();

            indexBuffer =
                    RenderSystem.getSequentialBuffer(
                            ESP_LINES.getVertexFormatMode()
                    ).getBuffer(
                            drawState.indexCount()
                    );
        }

        RenderPass pass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> "shafting_esp",
                        client.getMainRenderTarget().getColorTextureView(),
                        Optional.empty()
                );

        try (pass) {

            pass.setPipeline(ESP_LINES);

            pass.bindVertexBuffer(
                    0,
                    vertices
            );

            if (indexBuffer != null && indexType != null) {
                pass.bindIndexBuffer(
                        indexBuffer,
                        indexType
                );
            }

            Matrix4fc modelView =
                    RenderSystem.getModelViewMatrix();

            pass.setUniform(
                    "ModelViewMat",
                    modelView
            );

            pass.setUniform(
                    "ProjMat",
                    RenderSystem.getProjectionMatrix()
            );

            if (indexBuffer != null) {
                pass.drawIndexed(
                        0,
                        0,
                        drawState.indexCount(),
                        1
                );
            } else {
                pass.draw(
                        0,
                        drawState.vertexCount()
                );
            }
        }
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
