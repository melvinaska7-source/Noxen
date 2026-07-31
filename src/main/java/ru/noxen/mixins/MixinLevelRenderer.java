package noxen.inject.render;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LevelRenderer;
import net.minecraft.client.util.math.MatrixStack;
import noxen.client.features.modules.render.SkyShaderModule;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {

    @Inject(method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V", at = @At("TAIL"))
    private void onRenderSky(MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo ci) {
        SkyShaderModule module = SkyShaderModule.getInstance();
        if (module != null && module.isEnabled()) {
            module.renderSkyShader(tickDelta);
        }
    }
}
