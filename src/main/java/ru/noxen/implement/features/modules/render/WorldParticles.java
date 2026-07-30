
package ru.noxen.implement.features.modules.render;

import ru.noxen.api.feature.module.Module;
import ru.noxen.api.feature.module.ModuleCategory;
import ru.noxen.api.feature.module.setting.implement.SelectSetting;
import ru.noxen.api.feature.module.setting.implement.ValueSetting;
import ru.noxen.common.QuickImports;
import ru.noxen.common.util.color.ColorUtil;
import ru.noxen.implement.events.render.WorldRenderEvent;
import ru.noxen.implement.events.player.TickEvent;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.noxen.api.event.EventHandler;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class WorldParticles extends Module implements QuickImports {

    public static WorldParticles getInstance() {
        return ru.noxen.common.util.other.Instance.get(WorldParticles.class);
    }

    public final SelectSetting typeSetting = new SelectSetting("Вид", "Выбери текстуру")
            .value("Доллары", "Снежинки", "Орбизы", "Звёзды", "Пузыри");

    public final ValueSetting countSetting = new ValueSetting("Макс. частиц", "Сколько летает вокруг").range(5, 80).setValue(30);
    public final ValueSetting radiusSetting = new ValueSetting("Радиус", "Радиус появления").range(1, 8).setValue(3);
    public final ValueSetting speedSetting = new ValueSetting("Скорость", "Скорость движения частиц").range(0.1f, 3.0f).setValue(1.0f);

    private final CopyOnWriteArrayList<Particle> particles = new CopyOnWriteArrayList<>();

    public WorldParticles() {
        super("WorldParticles", "WorldParticles", ModuleCategory.RENDER);
        setup(typeSetting, countSetting, radiusSetting, speedSetting);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        int max = (int) countSetting.getValue();
        float radius = (float) radiusSetting.getValue();

        // Спавним новые частицы если мало
        while (particles.size() < max) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            double angle = r.nextDouble() * Math.PI * 2;
            double dist = r.nextDouble() * radius;
            Vec3d spawnPos = mc.player.getPos().add(
                    Math.cos(angle) * dist,
                    r.nextDouble() * 2.5,
                    Math.sin(angle) * dist
            );
            particles.add(new Particle(spawnPos));
        }

        // Удаляем мёртвые
        particles.removeIf(Particle::isDead);
    }

    @EventHandler
    public void onRender(WorldRenderEvent e) {
        if (mc.player == null) return;

        MatrixStack matrix = e.getStack();
        Identifier texture = getTexture();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_TEX_COLOR);

        for (Particle p : particles) {
            p.tick(e.getPartialTicks(), (float) speedSetting.getValue());

            matrix.push();
            matrix.translate(p.pos.x, p.pos.y, p.pos.z);
            matrix.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrix.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            float size = p.size;
            float alpha = p.getAlpha();
            int color = ColorUtil.multAlpha(ColorUtil.fade((int)(p.colorOffset)), alpha);
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            int a = (color >> 24) & 0xFF;

            Matrix4f mat = matrix.peek().getPositionMatrix();
            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            buf.vertex(mat, -size, -size, 0).texture(0, 0).color(r, g, b, a);
            buf.vertex(mat, -size,  size, 0).texture(0, 1).color(r, g, b, a);
            buf.vertex(mat,  size,  size, 0).texture(1, 1).color(r, g, b, a);
            buf.vertex(mat,  size, -size, 0).texture(1, 0).color(r, g, b, a);
            BufferRenderer.drawWithGlobalProgram(buf.end());

            matrix.pop();
        }

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private Identifier getTexture() {
        if (typeSetting.isSelected("Снежинки")) return Identifier.of("minecraft", "textures/snow.png");
        if (typeSetting.isSelected("Орбизы"))   return Identifier.of("minecraft", "textures/orbiz.png");
        if (typeSetting.isSelected("Звёзды"))   return Identifier.of("minecraft", "textures/star.png");
        if (typeSetting.isSelected("Пузыри"))   return Identifier.of("minecraft", "textures/bubble.png");
        return Identifier.of("minecraft", "textures/dollar.png");
    }

    private static class Particle {
        Vec3d pos;
        Vec3d velocity;
        float size;
        float life;
        float maxLife;
        float colorOffset;

        Particle(Vec3d origin) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            this.pos = origin;
            this.velocity = new Vec3d(
                    (r.nextDouble() - 0.5) * 0.02,
                    r.nextDouble() * 0.03 + 0.01,
                    (r.nextDouble() - 0.5) * 0.02
            );
            this.size = (float)(r.nextDouble() * 0.1 + 0.05);
            this.maxLife = r.nextFloat() * 60 + 40;
            this.life = maxLife;
            this.colorOffset = r.nextFloat() * 360;
        }

        void tick(float delta, float speedMultiplier) {
            pos = pos.add(velocity.multiply(delta * speedMultiplier));
            velocity = new Vec3d(
                    velocity.x * 0.99,
                    velocity.y - 0.001 * delta,
                    velocity.z * 0.99
            );
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
