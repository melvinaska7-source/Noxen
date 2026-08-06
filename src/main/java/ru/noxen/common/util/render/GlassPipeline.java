package ru.noxen.common.util.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import ru.noxen.api.system.shape.ShapeProperties;
import ru.noxen.common.QuickImports;
import ru.noxen.common.util.color.ColorUtil;

import java.awt.Color;

/**
 * "Liquid Glass" panel renderer for Noxen.
 *
 * NOTE: this file was rewritten to compile against Minecraft 1.21.4 (see
 * gradle.properties -> minecraft_version). The previous version used the
 * com.mojang.blaze3d.buffers/pipeline/systems/textures GPU-object API
 * (RenderPipeline, GpuBuffer, GpuTexture, CommandEncoder, RenderPass...).
 * Those classes don't exist until Minecraft 1.21.5 - that's the entire
 * reason the build was failing with "package ... does not exist" for every
 * single blaze3d import.
 *
 * Instead of building a whole new GPU capture+blur pipeline from scratch,
 * this reuses the project's own already-working "blur" Shape
 * (ru.noxen.api.system.shape.implement.Blur, backed by the existing
 * assets/minecraft/shaders/core/blur.json + .vsh + .fsh, same shader family
 * as the "round" shader Rectangle already uses for every non-glass panel).
 * That shader already does exactly what Liquid Glass needs: it samples the
 * framebuffer behind a rounded rect with a real multi-tap blur and blends in
 * a tint colour - a real backdrop blur, not a fake translucent overlay.
 */
public class GlassPipeline implements QuickImports {

    public static void draw(Matrix4f matrix, float x, float y, float width, float height, float radius,
                             float blurAmount, float distortion, float shine, Color tint, float alpha, float z) {
        if (alpha <= 0.01f || width <= 0.5f || height <= 0.5f) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() == null) return;

        // Snapshot whatever has been drawn to the screen so far this frame
        // (world + previously drawn UI) into blur.input, so the panel blurs
        // whatever is actually behind it right now.
        blur.setup();

        // The rest of the project's shape renderers (Rectangle, Blur, ...)
        // take a MatrixStack, but GlassPipeline.draw() is called everywhere
        // with just the position Matrix4f - wrap it instead of touching
        // every call site.
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.peek().getPositionMatrix().set(matrix);

        int clampedAlpha = Math.round(Math.max(0f, Math.min(1f, alpha)) * tint.getAlpha());
        int packedTint = ColorUtil.getColor(tint.getRed(), tint.getGreen(), tint.getBlue(), clampedAlpha);

        ShapeProperties props = ShapeProperties.create(matrixStack, x, y, width, height)
                .round(radius)
                .softness(1f)
                .thickness(0f)
                // blurAmount from call sites (e.g. 4-6) was tuned for a different
                // shader; multiplied up here so the panel actually reads as blurred
                // rather than just faintly tinted.
                .quality(Math.max(blurAmount * 5f, 4f))
                .color(packedTint)
                .build();

        // blur is a single shared instance used by every panel in the project
        // (glass and non-glass alike), so set distortion/shine only for this
        // draw and reset them right after - otherwise a later plain panel this
        // same frame would inherit our glass settings.
        blur.distortion = Math.max(distortion, 0f);
        // every current caller passes shine=0, but the rim-light highlight is
        // what actually reads as "glass" to the eye - give it a small floor.
        blur.shine = Math.max(shine, 1f);
        blur.render(props);
        blur.distortion = 0f;
        blur.shine = 0f;
    }
}
