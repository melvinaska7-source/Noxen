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

    ValueSetting radiusSetting = new ValueSetting("Radius", "Circle radius").setValue(1.2f).range(0.3f, 3.0f);
    ValueSetting lifetimeSetting = new ValueSetting("Lifetime", "Lifetime ms").setValue(2000).range(800, 4000);

    BooleanSetting useClientColor = new BooleanSetting("Client Color", "Use HUD color").setValue(true);
    ColorSetting colorSetting = new ColorSetting("Color", "Custom color")
            .setColor(0xFF6C9AFD)
            .visible(() -> !useClientColor.isValue());

    // используем glow.png — он у тебя точно есть
    Identifier texId = Identifier.of("textures/circle.png");

    CopyOnWriteArrayList<Circle> circles = new CopyOnWriteArrayList<>();

    public JumpCircle() {
        super("JumpCircle", "Круг при прыжке", ModuleCategory.RENDER);
        setup(radiusSetting, lifetimeSetting, useClientColor, colorSetting);
    }

    @EventHandler
    public void onJump(JumpEvent e) {
        if (mc.player == null) return;
        if (e.getPlayer() != mc.player) return;
        circles.add(new Circle(mc.player.getPos().add(0, 0.05, 0), System.currentTimeMillis()));
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.player == null || circles.isEmpty()) return;

        long now = System.currentTimeMillis();
        long life = (long) lifetimeSetting.getValue();
        float baseRadius = radiusSetting.getValue();
        int baseColor = useClientColor.isValue() ? ColorUtil.getClientColor() : colorSetting.getColor();

        MatrixStack matrices = e.getStack();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texId);

        for (Circle c : circles) {
            long age = now - c.time;
            if (age > life) {
                circles.remove(c);
                continue;
            }

            float progress = age / (float) life;
            float fade = 1f - progress;
            float rad = baseRadius * (0.25f + progress * 1.4f);

            // БЕЗ вычитания камеры
            float x = (float) (c.pos.x - rad / 2.0);
            float y = (float) c.pos.y;
            float z = (float) (c.pos.z - rad / 2.0);

            int color = ColorUtil.multAlpha(baseColor, fade * 0.9f);

            matrices.push();
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            buffer.vertex(matrix, x, y, z).texture(0, 0).color(color);
            buffer.vertex(matrix, x + rad, y, z).texture(1, 0).color(color);
            buffer.vertex(matrix, x + rad, y, z + rad).texture(1, 1).color(color);
            buffer.vertex(matrix, x, y, z + rad).texture(0, 1).color(color);
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
