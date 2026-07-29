package ru.noxen.implement.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.noxen.api.event.EventHandler;
import ru.noxen.api.feature.module.Module;
import ru.noxen.api.feature.module.ModuleCategory;
import ru.noxen.api.feature.module.setting.implement.BooleanSetting;
import ru.noxen.api.feature.module.setting.implement.ColorSetting;
import ru.noxen.api.feature.module.setting.implement.ValueSetting;
import ru.noxen.common.util.color.ColorUtil;
import ru.noxen.common.util.math.MathUtil;
import ru.noxen.common.util.other.Instance;
import ru.noxen.implement.events.render.WorldRenderEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChinaHat extends Module {

    public static ChinaHat getInstance() {
        return Instance.get(ChinaHat.class);
    }

    BooleanSetting self = new BooleanSetting("Self", "Show on yourself").setValue(true);
    BooleanSetting others = new BooleanSetting("Others", "Show on other players").setValue(true);

    ValueSetting width = new ValueSetting("Width", "Hat radius").setValue(0.6f).range(0.2f, 1.5f);
    ValueSetting height = new ValueSetting("Height", "Hat height").setValue(0.35f).range(0.1f, 0.8f);

    BooleanSetting useClientColor = new BooleanSetting("Client Color", "Use HUD color").setValue(true);
    ColorSetting colorSetting = new ColorSetting("Color", "Custom hat color")
            .setColor(0xFF6C9AFD)
            .presets(0xFF6C9AFD, 0xFF8C7FFF, 0xFFFFA576, 0xFFFF7B7B)
            .visible(() -> !useClientColor.isValue());

    ValueSetting alpha = new ValueSetting("Alpha", "Transparency").setValue(0.8f).range(0.15f, 1.0f);

    public ChinaHat() {
        super("ChinaHat", "Китайская шляпа", ModuleCategory.RENDER);
        setup(self, others, width, height, useClientColor, colorSetting, alpha);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.world == null || mc.player == null) return;

        MatrixStack matrices = e.getStack();
        float tickDelta = e.getPartialTicks();

        int baseColor = useClientColor.isValue() ? ColorUtil.getClientColor() : colorSetting.getColor();
        float aMul = alpha.getValue();

        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) {
                if (!self.isValue()) continue;
                if (mc.options.getPerspective().isFirstPerson()) continue;
            } else if (!others.isValue()) {
                continue;
            }
            if (!player.isAlive()) continue;

            renderHat(matrices, player, tickDelta, baseColor, aMul);
        }
    }

    private void renderHat(MatrixStack matrices, PlayerEntity player, float tickDelta, int color, float alphaMul) {
        // ВАЖНО: без вычитания камеры — stack уже сдвинут
        Vec3d pos = MathUtil.interpolate(player);

        float radius = width.getValue();
        float coneH = height.getValue();

        int r = ColorUtil.red(color);
        int g = ColorUtil.green(color);
        int b = ColorUtil.blue(color);
        int a = (int) (255 * alphaMul);

        matrices.push();
        matrices.translate(pos.x, pos.y + player.getHeight() + 0.05, pos.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        int segments = 32;

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < segments; i++) {
            float a1 = (float) (2 * Math.PI * i / segments);
            float a2 = (float) (2 * Math.PI * (i + 1) / segments);
            float x1 = radius * (float) Math.cos(a1);
            float z1 = radius * (float) Math.sin(a1);
            float x2 = radius * (float) Math.cos(a2);
            float z2 = radius * (float) Math.sin(a2);

            buffer.vertex(matrix, x1, 0, z1).color(r, g, b, a);
            buffer.vertex(matrix, x2, 0, z2).color(r, g, b, a);
            buffer.vertex(matrix, 0, coneH, 0).color(r, g, b, Math.min(255, a + 40));
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, a / 2);
        for (int i = 0; i <= segments; i++) {
            float ang = (float) (2 * Math.PI * i / segments);
            buffer.vertex(matrix, radius * (float) Math.cos(ang), 0, radius * (float) Math.sin(ang)).color(r, g, b, a / 2);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();
    }
}
