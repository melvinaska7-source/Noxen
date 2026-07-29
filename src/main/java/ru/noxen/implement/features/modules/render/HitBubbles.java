package ru.noxen.implement.features.modules.render;

import ru.noxen.api.feature.module.Module;
import ru.noxen.api.feature.module.ModuleCategory;
import ru.noxen.api.feature.module.setting.implement.ValueSetting;
import ru.noxen.common.QuickImports;
import ru.noxen.common.util.color.ColorUtil;
import ru.noxen.implement.events.player.AttackEvent;
import ru.noxen.implement.events.render.WorldRenderEvent;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.noxen.api.event.EventHandler;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class HitBubbles extends Module implements QuickImports {

    public static HitBubbles getInstance() {
        return ru.noxen.common.util.other.Instance.get(HitBubbles.class);
    }

    public final ValueSetting countSetting = new ValueSetting("Кол-во", "Пузырей за удар").range(1, 25).setValue(8);
    public final ValueSetting sizeSetting = new ValueSetting("Размер", "Размер пузыря").range(0.04f, 0.3f).setValue(0.12f);

    private final Identifier bubbleTexture = Identifier.of("minecraft", "textures/bubble.png");
    private final CopyOnWriteArrayList<Bubble> bubbles = new CopyOnWriteArrayList<>();

    public HitBubbles() {
        super("HitBubbles", "HitBubbles", ModuleCategory.RENDER);
        setup(countSetting, sizeSetting);
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        if (!(e.getEntity() instanceof LivingEntity target)) return;
        int count = (int) countSetting.getValue();
        float size = (float) sizeSetting.getValue();
        Vec3d origin = target.getPos().add(0, target.getHeight() / 2f, 0);
        for (int i = 0; i < count; i++) {
            bubbles.add(new Bubble(origin, size));
        }
    }

    @EventHandler
    public void onRender(WorldRenderEvent e) {
        if (bubbles.isEmpty()) return;

        MatrixStack matrix = e.getStack();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderTexture(0, bubbleTexture);
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_TEX_COLOR);

        bubbles.removeIf(b -> {
            b.tick(e.getPartialTicks());
            return b.isDead();
        });

        for (Bubble b : bubbles) {
            matrix.push();
            matrix.translate(b.pos.x, b.pos.y, b.pos.z);
            matrix.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrix.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            float alpha = b.getAlpha();
            int color = ColorUtil.multAlpha(ColorUtil.fade(0), alpha);
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int bl = color & 0xFF;
            int a = (color >> 24) & 0xFF;

            float s = b.size;
            Matrix4f mat = matrix.peek().getPositionMatrix();
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            buf.vertex(mat, -s, -s, 0).texture(0, 0).color(r, g, bl, a);
            buf.vertex(mat, -s,  s, 0).texture(0, 1).color(r, g, bl, a);
            buf.vertex(mat,  s,  s, 0).texture(1, 1).color(r, g, bl, a);
            buf.vertex(mat,  s, -s, 0).texture(1, 0).color(r, g, bl, a);
            BufferRenderer.drawWithGlobalProgram(buf.end());

            matrix.pop();
        }

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static class Bubble {
        Vec3d pos;
        Vec3d velocity;
        float size;
        float life;
        float maxLife;

        Bubble(Vec3d origin, float size) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            this.pos = origin;
            this.size = size * (float)(r.nextDouble() * 0.5 + 0.75);
            this.velocity = new Vec3d(
                    (r.nextDouble() - 0.5) * 0.1,
                    r.nextDouble() * 0.15 + 0.05,
                    (r.nextDouble() - 0.5) * 0.1
            );
            this.maxLife = r.nextFloat() * 15 + 10;
            this.life = maxLife;
        }

        void tick(float delta) {
            pos = pos.add(velocity.multiply(delta));
            velocity = new Vec3d(velocity.x * 0.95, velocity.y - 0.005 * delta, velocity.z * 0.95);
            life -= delta;
        }

        float getAlpha() {
            return Math.max(0, life / maxLife);
        }

        boolean isDead() {
            return life <= 0;
        }
    }
}

