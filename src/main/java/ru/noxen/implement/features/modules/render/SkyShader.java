package ru.noxen.implement.features.modules.render;

import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import ru.noxen.api.event.EventHandler;
import ru.noxen.api.feature.module.Module;
import ru.noxen.api.feature.module.ModuleCategory;
import ru.noxen.api.feature.module.setting.implement.SelectSetting;
import ru.noxen.api.feature.module.setting.implement.ValueSetting;
import ru.noxen.common.QuickImports;
import ru.noxen.common.util.color.ColorUtil;
import ru.noxen.implement.events.render.WorldRenderEvent;

public class SkyShader extends Module implements QuickImports {

    public static SkyShader getInstance() {
        return ru.noxen.common.util.other.Instance.get(SkyShader.class);
    }

    public final SelectSetting modeSetting = new SelectSetting("Mode", "Type").value("Waves", "Fire");
    public final ValueSetting speedSetting = new ValueSetting("Speed", "Animation Speed").range(0.1f, 5.0f).setValue(1.0f);
    public final ValueSetting scaleSetting = new ValueSetting("Size", "Size").range(1.0f, 20.0f).setValue(5.0f);
    public final ValueSetting intensitySetting = new ValueSetting("Intense", "Intense").range(0.001f, 0.05f).setValue(0.01f);
    public final ValueSetting alphaSetting = new ValueSetting("transparency", "Visibility").range(0.05f, 1.0f).setValue(1.0f);

    private final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(
            Identifier.of("minecraft", "core/skyshader"), VertexFormats.POSITION, Defines.EMPTY);

    private long startMillis = -1;

    public SkyShader() {
        super("SkyShader", "SkyShader", ModuleCategory.RENDER);
        setup(modeSetting, speedSetting, scaleSetting, intensitySetting, alphaSetting);
    }

    @Override
    public void deactivate() {
        startMillis = -1;
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (startMillis < 0) startMillis = System.currentTimeMillis();

        float time = (System.currentTimeMillis() - startMillis) / 1000f;
        float fw = mc.getWindow().getFramebufferWidth();
        float fh = mc.getWindow().getFramebufferHeight();

        int themeColor = ColorUtil.getClientColor();
        float cr = ColorUtil.redf(themeColor);
        float cg = ColorUtil.greenf(themeColor);
        float cb = ColorUtil.bluef(themeColor);

        float yaw = (float) Math.toRadians(-mc.gameRenderer.getCamera().getYaw());
        float pitch = (float) Math.toRadians(mc.gameRenderer.getCamera().getPitch());
        float fov = (float) mc.options.getFov().getValue().intValue();

        ShaderProgram shader = RenderSystem.setShader(SHADER_KEY);
        if (shader == null) return;

        shader.getUniformOrDefault("uTime").set(time);
        shader.getUniformOrDefault("uResolution").set(fw, fh);
        shader.getUniformOrDefault("uColor").set(cr, cg, cb);
        shader.getUniformOrDefault("uAlpha").set((float) alphaSetting.getValue());
        shader.getUniformOrDefault("uSpeed").set((float) speedSetting.getValue());
        shader.getUniformOrDefault("uScale").set((float) scaleSetting.getValue());
        shader.getUniformOrDefault("uIntensity").set((float) intensitySetting.getValue());
        shader.getUniformOrDefault("uCameraDir").set(yaw, pitch);
        shader.getUniformOrDefault("uFov").set(fov);
        shader.getUniformOrDefault("uMode").set(modeSetting.isSelected("Fire") ? 1.0f : 0.0f);

        // Временно подменяем проекцию на ортографическую, рисуем квад в clip-space
        // на дальней плоскости (z=1). Тест глубины GL_EQUAL + отключённая запись
        // глубины гарантирует, что эффект появится только там, где ничего не
        // отрисовано перед этим (то есть на месте неба), не перекрывая террейн.
        Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        RenderSystem.setProjectionMatrix(new Matrix4f(), ProjectionType.ORTHOGRAPHIC);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_EQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        Matrix4f identity = new Matrix4f();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        buf.vertex(identity, -1f, -1f, 1f);
        buf.vertex(identity, 1f, -1f, 1f);
        buf.vertex(identity, 1f, 1f, 1f);
        buf.vertex(identity, -1f, 1f, 1f);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.setProjectionMatrix(savedProj, ProjectionType.PERSPECTIVE);
    }
}
