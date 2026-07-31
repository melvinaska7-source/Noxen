package ru.noxen.implement.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;
import noxen.client.features.modules.Module;

import java.awt.Color;

public class SkyShaderModule extends Module {
    private static SkyShaderModule INSTANCE;
    private PostEffectProcessor skyShader;
    private float time = 0.0f;

    // Параметры кастомизации
    public Color color1 = new Color(40, 100, 220);
    public Color color2 = new Color(200, 50, 150);
    public float speed = 0.8f;
    public float scale = 2.5f;

    public SkyShaderModule() {
        super("SkyShader", "Красивый анимированный шейдер неба", Category.RENDER);
        INSTANCE = this;
    }

    public static SkyShaderModule getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SkyShaderModule();
        }
        return INSTANCE;
    }

    public void renderSkyShader(float tickDelta) {
        if (!this.isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        if (skyShader == null) {
            try {
                // В 1.21.4 вместо new Identifier() используется Identifier.of()
                Identifier shaderId = Identifier.of("minecraft", "shaders/core/water.json");
                skyShader = new PostEffectProcessor(
                    client.getTextureManager(), 
                    client.getResourceManager(), 
                    client.getFramebuffer(), 
                    shaderId
                );
                skyShader.setupDimensions(client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight());
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        time += tickDelta * 0.05f * speed;

        try {
            // Передаем юниформы в шейдер
            var pass = skyShader.passes.get(0);
            if (pass != null && pass.getProgram() != null) {
                if (pass.getProgram().getUniformByName("Time") != null)
                    pass.getProgram().getUniformByName("Time").set(time);
                if (pass.getProgram().getUniformByName("Speed") != null)
                    pass.getProgram().getUniformByName("Speed").set(speed);
                if (pass.getProgram().getUniformByName("Scale") != null)
                    pass.getProgram().getUniformByName("Scale").set(scale);
                if (pass.getProgram().getUniformByName("Color1") != null)
                    pass.getProgram().getUniformByName("Color1").set(color1.getRed() / 255f, color1.getGreen() / 255f, color1.getBlue() / 255f);
                if (pass.getProgram().getUniformByName("Color2") != null)
                    pass.getProgram().getUniformByName("Color2").set(color2.getRed() / 255f, color2.getGreen() / 255f, color2.getBlue() / 255f);
            }

            skyShader.render(tickDelta);
        } catch (Exception ignored) {}
    }
}
