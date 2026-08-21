package com.shafting.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
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

public final class WorldEspRenderer {

    private static final RenderPipeline ESP_PIPELINE =
            RenderPipelines.register(
                    RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                            .withLocation(
                                    Identifier.fromNamespaceAndPath(
                                            "shafting",
                                            "pipeline/esp_box"
                                    )
                            )
                            .withDepthStencilState(Optional.empty())
                            .build()
            );

    private static final ByteBufferBuilder ALLOCATOR =
            new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);

    private static final Vector4f COLOR_MODULATOR =
            new Vector4f(1f, 1f, 1f, 1f);

    private static final Vector3f MODEL_OFFSET =
            new Vector3f();

    private static final Matrix4f TEXTURE_MATRIX =
            new Matrix4f();

    private static final List<EspBox> BOXES =
            new ArrayList<>();

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
        BOXES.clear();

        if (ShaftingClient.config.littlefootEsp) {
            for (Entity entity : context.level().entitiesForRendering()) {
                if (!isLittlefoot(entity)) {
                    continue;
                }

                AABB box = entity
                        .getBoundingBox()
                        .inflate(0.08);

                BOXES.add(
                        new EspBox(
                                box,
                                1.0f,
                                0.15f,
                                0.9f,
                                1.0f
                        )
                );
            }
        }

        if (ShaftingClient.config.corpseHighlight
                && AreaDetector.isMineshaft()) {

            for (Entity entity : context.level().entitiesForRendering()) {

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

                BOXES.add(
                        new EspBox(
                                box,
                                red(color),
                                green(color),
                                blue(color),
                                1.0f
                        )
                );
            }
        }
    }

    private static void render(LevelRenderContext context) {
        if (BOXES.isEmpty()) {
            return;
        }

        PoseStack matrices = context.poseStack();

        var camera =
                context.levelState()
                        .cameraRenderState
                        .pos;

        matrices.pushPose();

        matrices.translate(
                -camera.x,
                -camera.y,
                -camera.z
        );

        if (buffer == null) {
            buffer = new BufferBuilder(
                    ALLOCATOR,
                    ESP_PIPELINE.getVertexFormatMode(),
                    ESP_PIPELINE.getVertexFormat()
            );
        }

        Matrix4fc matrix = matrices.last().pose();

        for (EspBox esp : BOXES) {
            drawOutline(
                    matrix,
                    buffer,
                    esp.box(),
                    esp.r(),
                    esp.g(),
                    esp.b(),
                    esp.a()
            );
        }

        matrices.popPose();

        drawBuffer(
                Minecraft.getInstance(),
                ESP_PIPELINE
        );
    }

    private static boolean isLittlefoot(Entity entity) {
        if (!(entity instanceof Player)) {
            return false;
        }

        String name =
                entity.getName()
                        .getString()
                        .trim();

        String custom =
                entity.getCustomName() == null
                        ? ""
                        : entity.getCustomName()
                                .getString()
                                .trim();

        String display =
                entity.getDisplayName()
                        .getString()
                        .trim();

        return name.equalsIgnoreCase("Littlefoot")
                || custom.equalsIgnoreCase("Littlefoot")
                || display.equalsIgnoreCase("Littlefoot");
    }

    /*
     * Draws a thin 3D rectangular prism along each edge of the AABB.
     *
     * This gives us an outline while still using
     * DEBUG_FILLED_SNIPPET, which is supported by the
     * Minecraft 26.1.2 rendering pipeline.
     */
    private static void drawOutline(
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

        float width = 0.025f;

        /*
         * Bottom X edges
         */
        drawFilledBox(
                matrix,
                builder,
                minX,
                minY,
                minZ,
                maxX,
                minY + width,
                minZ + width,
                r, g, b, a
        );

        drawFilledBox(
                matrix,
                builder,
                minX,
                minY,
                maxZ - width,
                maxX,
                minY + width,
                maxZ,
                r, g, b, a
        );

        /*
         * Top X edges
         */
        drawFilledBox(
                matrix,
                builder,
                minX,
                maxY - width,
                minZ,
                maxX,
                maxY,
                minZ + width,
                r, g, b, a
        );

        drawFilledBox(
                matrix,
                builder,
                minX,
                maxY - width,
                maxZ - width,
                maxX,
                maxY,
                maxZ,
                r, g, b, a
        );

        /*
         * Bottom Z edges
         */
        drawFilledBox(
                matrix,
                builder,
                minX,
                minY,
                minZ,
                minX + width,
                minY + width,
                maxZ,
                r, g, b, a
        );

        drawFilledBox(
                matrix,
                builder,
                maxX - width,
                minY,
                minZ,
                maxX,
                minY + width,
                maxZ,
                r, g, b, a
        );

        /*
         * Top Z edges
         */
        drawFilledBox(
                matrix,
                builder,
                minX,
                maxY - width,
                minZ,
                minX + width,
                maxY,
                maxZ,
                r, g, b, a
        );

        drawFilledBox(
                matrix,
                builder,
                maxX - width,
                maxY - width,
                minZ,
                maxX,
                maxY,
                maxZ,
                r, g, b, a
        );

        /*
         * Vertical edges
         */
        drawFilledBox(
                matrix,
                builder,
                minX,
                minY,
                minZ,
                minX + width,
                maxY,
                minZ + width,
                r, g, b, a
        );

        drawFilledBox(
                matrix,
                builder,
                maxX - width,
                minY,
                minZ,
                maxX,
                maxY,
                minZ + width,
                r, g, b, a
        );

        drawFilledBox(
                matrix,
                builder,
                maxX - width,
                minY,
                maxZ - width,
                maxX,
                maxY,
                maxZ,
                r, g, b, a
        );

        drawFilledBox(
                matrix,
                builder,
                minX,
                minY,
                maxZ - width,
                minX + width,
                maxY,
                maxZ,
                r, g, b, a
        );
    }

    private static void drawFilledBox(
            Matrix4fc matrix,
            BufferBuilder builder,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            float r,
            float g,
            float b,
            float a
    ) {
        /*
         * Front
         */
        builder.addVertex(
                matrix,
                minX, minY, maxZ
        ).setColor(r, g, b, a);

        builder.addVertex(
                matrix,
                maxX, minY, maxZ
        ).setColor(r, g, b, a);

        builder.addVertex(
                matrix,
                maxX, maxY, maxZ
        ).setColor(r, g, b, a);

        builder.addVertex(
                matrix,
                minX, maxY, maxZ
        ).setColor(r, g, b, a);

        /*
         * Back
         */
        builder.addVertex(
                matrix,
                maxX, minY, minZ
        ).setColor(r, g, b, a);

        builder.addVertex(
                matrix,
                minX, minY, minZ
        ).setColor(r, g, b, a);

        builder.addVertex(
                matrix,
                minX, maxY, minZ
        ).setColor(r, g, b, a);

        builder.addVertex(
                matrix,
                maxX, maxY, minZ
        ).setColor(r, g, b, a);

        /*
         * Left
         */
        builder.addVertex(
                matrix,
                minX, minY, minZ
        ).setColor(r, g, b, a);

        builder.addVertex(
                matrix,
                minX, minY, maxZ
        ).setColor(r, g, b, a);

        builder.addVertex(
                matrix,
                minX, maxY, maxZ
        ).setColor(r, g, b, a);

        builder.addVertex(
                matrix,
                minX, maxY, minZ
        ).setColor(r, g, b, a);

        /*
         * Right
         */
        builder.addVertex(
                matrix,
                maxX, minY, maxZ
        ).setColor(r, g, b, a);

        builder.addVertex(
                matrix,
                maxX, minY, minZ
        ).setColor(r, g, b, a);

        builder.addVertex(
                matrix,
                maxX, maxY, minZ
        ).setColor(r, g, b, a);

        builder.addVertex(
                matrix,
                maxX, maxY, maxZ
        ).setColor(r, g, b, a);

        /*
         * Top
         */
        builder.addVertex(
                matrix,
                minX, maxY, maxZ
        ).setColor(r, g, b, a);

        builder.addVertex(
                matrix,
                maxX, maxY, maxZ
        ).setColor(r, g, b, a);

        builder.addVertex(
                matrix,
                maxX, maxY, minZ
        ).setColor(r, g, b, a);

        builder.addVertex(
                matrix,
                minX, maxY, minZ
        ).setColor(r, g, b, a);

        /*
         * Bottom
         */
        builder.addVertex(
                matrix,
                minX, minY, minZ
        ).setColor(r, g, b, a);

        builder.addVertex(
                matrix,
                maxX, minY, minZ
        ).setColor(r, g, b, a);

        builder.addVertex(
                matrix,
                maxX, minY, maxZ
        ).setColor(r, g, b, a);

        builder.addVertex(
                matrix,
                minX, minY, maxZ
        ).setColor(r, g, b, a);
    }

    private static void drawBuffer(
            Minecraft client,
            RenderPipeline pipeline
    ) {
        if (buffer == null) {
            return;
        }

        MeshData builtBuffer =
                buffer.buildOrThrow();

        MeshData.DrawState drawParameters =
                builtBuffer.drawState();

        VertexFormat format =
                drawParameters.format();

        GpuBuffer vertices =
                upload(
                        drawParameters,
                        format,
                        builtBuffer
                );

        draw(
                client,
                pipeline,
                builtBuffer,
                drawParameters,
                vertices,
                format
        );

        if (vertexBuffer != null) {
            vertexBuffer.rotate();
        }

        buffer = null;
    }

    private static GpuBuffer upload(
            MeshData.DrawState drawParameters,
            VertexFormat format,
            MeshData builtBuffer
    ) {
        int vertexBufferSize =
                drawParameters.vertexCount()
                        * format.getVertexSize();

        if (vertexBuffer == null
                || vertexBuffer.size() < vertexBufferSize) {

            if (vertexBuffer != null) {
                vertexBuffer.close();
            }

            vertexBuffer =
                    new MappableRingBuffer(
                            () -> "shafting_esp",
                            GpuBuffer.USAGE_VERTEX
                                    | GpuBuffer.USAGE_MAP_WRITE,
                            vertexBufferSize
                    );
        }

        CommandEncoder commandEncoder =
                RenderSystem.getDevice()
                        .createCommandEncoder();

        try (
                GpuBuffer.MappedView mappedView =
                        commandEncoder.mapBuffer(
                                vertexBuffer
                                        .currentBuffer()
                                        .slice(
                                                0,
                                                builtBuffer
                                                        .vertexBuffer()
                                                        .remaining()
                                        ),
                                false,
                                true
                        )
        ) {
            MemoryUtil.memCopy(
                    builtBuffer.vertexBuffer(),
                    mappedView.data()
            );
        }

        return vertexBuffer.currentBuffer();
    }

    private static void draw(
            Minecraft client,
            RenderPipeline pipeline,
            MeshData builtBuffer,
            MeshData.DrawState drawParameters,
            GpuBuffer vertices,
            VertexFormat format
    ) {
        GpuBuffer indices;
        VertexFormat.IndexType indexType;

        if (pipeline.getVertexFormatMode()
                == VertexFormat.Mode.QUADS) {

            builtBuffer.sortQuads(
                    ALLOCATOR,
                    RenderSystem
                            .getProjectionType()
                            .vertexSorting()
            );

            indices =
                    pipeline.getVertexFormat()
                            .uploadImmediateIndexBuffer(
                                    builtBuffer.indexBuffer()
                            );

            indexType =
                    drawParameters.indexType();

        } else {

            RenderSystem.AutoStorageIndexBuffer
                    shapeIndexBuffer =
                    RenderSystem.getSequentialBuffer(
                            pipeline.getVertexFormatMode()
                    );

            indices =
                    shapeIndexBuffer.getBuffer(
                            drawParameters.indexCount()
                    );

            indexType =
                    shapeIndexBuffer.type();
        }

        GpuBufferSlice dynamicTransforms =
                RenderSystem.getDynamicUniforms()
                        .writeTransform(
                                RenderSystem.getModelViewMatrix(),
                                COLOR_MODULATOR,
                                MODEL_OFFSET,
                                TEXTURE_MATRIX
                        );

        try (
                RenderPass renderPass =
                        RenderSystem
                                .getDevice()
                                .createCommandEncoder()
                                .createRenderPass(
                                        () -> "shafting esp rendering",
                                        client
                                                .getMainRenderTarget()
                                                .getColorTextureView(),
                                        OptionalInt.empty(),
                                        client
                                                .getMainRenderTarget()
                                                .getDepthTextureView(),
                                        OptionalDouble.empty()
                                )
        ) {
            renderPass.setPipeline(pipeline);

            RenderSystem.bindDefaultUniforms(
                    renderPass
            );

            renderPass.setUniform(
                    "DynamicTransforms",
                    dynamicTransforms
            );

            renderPass.setVertexBuffer(
                    0,
                    vertices
            );

            renderPass.setIndexBuffer(
                    indices,
                    indexType
            );

            renderPass.drawIndexed(
                    0,
                    0,
                    drawParameters.indexCount(),
                    1
            );
        }

        builtBuffer.close();
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

    private record EspBox(
            AABB box,
            float r,
            float g,
            float b,
            float a
    ) {
    }
}
