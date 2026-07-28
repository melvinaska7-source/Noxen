package ru.noxen.implement.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.noxen.api.event.EventHandler;
import ru.noxen.api.feature.module.Module;
import ru.noxen.api.feature.module.ModuleCategory;
import ru.noxen.api.feature.module.setting.implement.BooleanSetting;
import ru.noxen.api.feature.module.setting.implement.ColorSetting;
import ru.noxen.api.feature.module.setting.implement.ValueSetting;
import ru.noxen.common.util.color.ColorUtil;
import ru.noxen.common.util.other.Instance;
import ru.noxen.implement.events.player.JumpEvent;
import ru.noxen.implement.events.render.WorldRenderEvent;

import java.util.concurrent.CopyOnWriteArrayList;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JumpCircle extends Module {

    public static JumpCircle getInstance() {
        return Instance.get(JumpCircle.class);
    }

    ValueSetting radiusSetting = new ValueSetting("Radius", "Circle radius").setValue(1.0f).range(0.3f, 2.5f);
    ValueSetting lifetimeSetting = new ValueSetting("Lifetime", "How long circle lives (ms)").setValue(2000).range(800, 4000);

    BooleanSetting useClientColor = new BooleanSetting("Client Color", "Use HUD color").setValue(true);
    ColorSetting colorSetting = new ColorSetting("Color", "Custom color")
            .setColor(0xFF6C9AFD)
            .visible(() -> !useClientColor.isValue());

    Identifier circleId = Identifier.of("textures/circle.png");
    // fallback если circle.png нет:
    Identifier glowId = Identifier.of("textures/glow.png");

    CopyOnWriteArrayList<Circle> circles = new CopyOnWriteArrayList<>();

    public JumpCircle() {
        super("JumpCircle", "Jump Circle", ModuleCategory.RENDER);
        setup(radiusSetting, lifetimeSetting, useClientColor, colorSetting);
    }

    @EventHandler
    public void onJump(JumpEvent e) {
        if (mc.player == null) return;
        Vec3d pos = mc.player.getPos().add(0, 0.05, 0);
        circles.add(new Circle(pos, System.currentTimeMillis()));
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.player == null || circles.isEmpty()) return;

        long now = System.currentTimeMillis();
        long life = (long) lifetimeSetting.getValue();
        float baseRadius = radiusSetting.getValue();
        int baseColor = useClientColor.isValue() ? ColorUtil.getClientColor() : colorSetting.getColor();

        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        MatrixStack matrices = e.getStack();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        Identifier tex = circleId;
        // если хочешь всегда glow — замени на glowId
        RenderSystem.setShaderTexture(0, tex);

        for (Circle c : circles) {
            long age = now - c.time;
            if (age > life) {
                circles.remove(c);
                continue;
            }

            float progress = age / (float) life;
            float fade = 1f - progress;
            float rad = baseRadius * (0.3f + progress * 1.2f);

            double x = c.pos.x - camera.x - rad / 2.0;
            double y = c.pos.y - camera.y;
            double z = c.pos.z - camera.z - rad / 2.0;

            int color = ColorUtil.multAlpha(baseColor, fade);

            matrices.push();
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            buffer.vertex(matrix, (float) x, (float) y, (float) z).texture(0, 0).color(color);
            buffer.vertex(matrix, (float) (x + rad), (float) y, (float) z).texture(1, 0).color(color);
            buffer.vertex(matrix, (float) (x + rad), (float) y, (float) (z + rad)).texture(1, 1).color(color);
            buffer.vertex(matrix, (float) x, (float) y, (float) (z + rad)).texture(0, 1).color(color);
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            matrices.pop();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static class Circle {
        final Vec3d pos;
        final long time;

        Circle(Vec3d pos, long time) {
            this.pos = pos;
            this.time = time;
        }
    }
}
