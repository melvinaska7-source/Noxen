package ru.noxen.implement.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import ru.noxen.api.event.EventHandler;
import ru.noxen.api.feature.module.Module;
import ru.noxen.api.feature.module.ModuleCategory;
import ru.noxen.api.feature.module.setting.implement.SelectSetting;
import ru.noxen.api.feature.module.setting.implement.ValueSetting;
import ru.noxen.common.QuickImports;
import ru.noxen.common.util.color.ColorUtil;
import ru.noxen.implement.events.render.DrawEvent;

public class SkyShader extends Module implements QuickImports {

    public static SkyShader getInstance() {
        return ru.noxen.common.util.other.Instance.get(SkyShader.class);
    }

    public final SelectSetting modeSetting = new SelectSetting("Режим", "Тип эффекта").value("Вода", "Каустика");
    public final ValueSetting speedSetting = new ValueSetting("Скорость", "Скорость анимации").range(0.1f, 5.0f).setValue(1.0f);
    public final ValueSetting scaleSetting = new ValueSetting("Масштаб", "Масштаб узора").range(1.0f, 20.0f).setValue(5.0f);
    public final ValueSetting intensitySetting = new ValueSetting("Интенсивность", "Сила искажения").range(0.001f, 0.05f).setValue(0.01f);
    public final ValueSetting alphaSetting = new ValueSetting("Прозрачность", "Насколько заметен эффект").range(0.05f, 1.0f).setValue(0.35f);

    private final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(
            Identifier.of("minecraft", "core/skyshader"), VertexFormats.POSITION, Defines.EMPTY);

    private long startMillis = -1;

    public SkyShader() {
        super("SkyShader", "Небесный шейдер", ModuleCategory.RENDER);
        setup(modeSetting, speedSetting, scaleSetting, intensitySetting, alphaSetting);
    }

    @Override
    public void deactivate() {
        startMillis = -1;
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (startMillis < 0) startMillis = System.currentTimeMillis();

        MatrixStack matrix = e.getDrawContext().getMatrices();
        float width = mc.getWindow().getScaledWidth();
        float height = mc.getWindow().getScaledHeight();

        float time = (System.currentTimeMillis() - startMillis) / 1000f;
        float yaw = (float) Math.toRadians(mc.gameRenderer.getCamera().getYaw());
        float pitch = (float) Math.toRadians(mc.gameRenderer.getCamera().getPitch());
        float fov = (float) mc.options.getFov().getValue();

        int themeColor = ColorUtil.getClientColor();
        float cr = ColorUtil.redf(themeColor);
        float cg = ColorUtil.greenf(themeColor);
        float cb = ColorUtil.bluef(themeColor);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        Matrix4f posMatrix = matrix.peek().getPositionMatrix();
        drawEngine.quad(posMatrix, buffer, 0, 0, width, height);

        ShaderProgram shader = RenderSystem.setShader(SHADER_KEY);
        shader.getUniformOrDefault("uTime").set(time);
        shader.getUniformOrDefault("uResolution").set((float) mc.getWindow().getFramebufferWidth(), (float) mc.getWindow().getFramebufferHeight());
        shader.getUniformOrDefault("uColor").set(cr, cg, cb);
        shader.getUniformOrDefault("uAlpha").set((float) alphaSetting.getValue());
        shader.getUniformOrDefault("uSpeed").set((float) speedSetting.getValue());
        shader.getUniformOrDefault("uScale").set((float) scaleSetting.getValue());
        shader.getUniformOrDefault("uIntensity").set((float) intensitySetting.getValue());
        shader.getUniformOrDefault("uCameraDir").set(yaw, pitch);
        shader.getUniformOrDefault("uFov").set(fov);
        shader.getUniformOrDefault("uMode").set(modeSetting.isSelected("Каустика") ? 1.0f : 0.0f);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
