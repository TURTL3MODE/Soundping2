package com.example.soundping;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public class SoundPingMod implements ClientModInitializer {
    private static final RenderPipeline THROUGH_WALLS = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("soundping", "pipeline/ping_box"))
                    .withDepthStencilState(Optional.empty())
                    .build());

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final StagedVertexBuffer STAGED_BUFFER =
            new StagedVertexBuffer(() -> "SoundPing Buffer", RenderType.SMALL_BUFFER_SIZE);

    private record BoxState(int x, int y, int z, float r, float g, float b, float a) {}
    private volatile List<BoxState> boxes = List.of();

    @Override
    public void onInitializeClient() {
        LevelExtractionEvents.END_EXTRACTION.register(this::extract);
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(this::renderAndDraw);
    }

    private void extract(LevelExtractionContext ctx) {
        long now = System.currentTimeMillis();
        SoundPings.PINGS.removeIf(p -> now - p.createdMs() > SoundPings.LIFETIME_MS);
        boxes = SoundPings.PINGS.stream().map(p -> {
            float age = (now - p.createdMs()) / (float) SoundPings.LIFETIME_MS;
            float alpha = Math.max(0.15f, 0.45f * (1f - age));
            return new BoxState(p.pos().getX(), p.pos().getY(), p.pos().getZ(), p.r(), p.g(), p.b(), alpha);
        }).toList();
    }

    private void renderAndDraw(LevelRenderContext context) {
        List<BoxState> snapshot = boxes;
        if (snapshot.isEmpty()) return;

        RenderPipeline pipeline = THROUGH_WALLS;
        VertexFormat format = pipeline.getVertexFormatBinding(0);
        assert format != null;
        PrimitiveTopology prim = pipeline.getPrimitiveTopology();

        StagedVertexBuffer.Draw draw = STAGED_BUFFER.appendDraw(format, prim,
                prim == PrimitiveTopology.QUADS ? RenderSystem.getProjectionType().vertexSorting() : null);

        PoseStack ps = context.poseStack();
        Vec3 cam = context.levelState().cameraRenderState.pos;
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        VertexConsumer vc = STAGED_BUFFER.getVertexBuilder(draw);
        for (BoxState b : snapshot) {
            float e = 0.002f;
            box(ps.last().pose(), vc, b.x() - e, b.y() - e, b.z() - e,
                    b.x() + 1 + e, b.y() + 1 + e, b.z() + 1 + e, b.r(), b.g(), b.b(), b.a());
        }
        ps.popPose();

        STAGED_BUFFER.upload();
        StagedVertexBuffer.ExecuteInfo info = STAGED_BUFFER.getExecuteInfo(draw);
        if (info != null) submit(Minecraft.getInstance(), info, pipeline);
        STAGED_BUFFER.endFrame();
    }

    private static void box(Matrix4fc m, VertexConsumer v, float x0, float y0, float z0,
                            float x1, float y1, float z1, float r, float g, float b, float a) {
        float[][] f = {
            {x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1},
            {x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0},
            {x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0},
            {x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1},
            {x0,y1,z1, x1,y1,z1, x1,y1,z0, x0,y1,z0},
            {x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1}
        };
        for (float[] q : f)
            for (int i = 0; i < 12; i += 3)
                v.addVertex(m, q[i], q[i + 1], q[i + 2]).setColor(r, g, b, a);
    }

    private static void submit(Minecraft client, StagedVertexBuffer.ExecuteInfo info, RenderPipeline pipeline) {
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        RenderTarget target = client.gameRenderer.mainRenderTarget();
        GpuTextureView color = target.getColorTextureView();
        assert color != null;

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "soundping boxes", color, Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.setVertexBuffer(0, info.vertexBuffer().slice());
            pass.setIndexBuffer(info.indexBuffer(), info.indexType());
            pass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }

    public static void close() { STAGED_BUFFER.close(); }
}
