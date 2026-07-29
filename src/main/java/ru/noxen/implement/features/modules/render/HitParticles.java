package ru.noxen.implement.features.modules.render;

import ru.noxen.api.feature.module.Module;
import ru.noxen.api.feature.module.ModuleCategory;
import ru.noxen.api.feature.module.setting.implement.MultiSelectSetting;
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

public class HitParticles extends Module implements QuickImports {

    public static HitParticles getInstance() {
        return ru.noxen.common.util.other.Instance.get(HitParticles.class);
    }

    public final MultiSelectSetting typeSetting = new MultiSelectSetting("Вид", "Выбери текстуру")
            .value("Доллары", "Снежинки", "Орбизы", "Звёзды", "Пузыри");

    public final ValueSetting countSetting = new ValueSetting("Кол-во", "Частиц за удар").range(1, 40).setValue(15);

    private final CopyOnWriteArrayList<Particle> particles = new CopyOnWriteArrayList<>();

    public HitParticles() {
        super("HitParticles", "HitParticles", ModuleCategory.RENDER);
        setup(typeSetting, countSetting);
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        if (!(e.getEntity() instanceof LivingEntity target)) return;
        int count = (int) countSetting.getValue();
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(target.getPos().add(0, target.getHeight() / 2f, 0)));
        }
    }

    @EventHandler
    public void onRender(WorldRenderEvent e) {
        MatrixStack matrix = e.getStack();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();

        Identifier texture = getTexture();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_TEX_COLOR);

        particles.removeIf(p -> {
            p.tick(e.getPartialTicks());
            return p.isDead();
        });

        for (Particle p : particles) {
            matrix.push();
            matrix.translate(p.pos.x - cam.x, p.pos.y - cam.y, p.pos.z - cam.z);
            matrix.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
            matrix.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

            float size = p.size;
            float alpha = p.getAlpha();
            int color = ColorUtil.multAlpha(ColorUtil.fade(0), alpha);
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

        Particle(Vec3d origin) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            this.pos = origin;
            this.velocity = new Vec3d(
                    (r.nextDouble() - 0.5) * 0.15,
                    r.nextDouble() * 0.2 + 0.05,
                    (r.nextDouble() - 0.5) * 0.15
            );
            this.size = (float)(r.nextDouble() * 0.12 + 0.06);
            this.maxLife = r.nextFloat() * 20 + 15;
            this.life = maxLife;
        }

        void tick(float delta) {
            pos = pos.add(velocity.multiply(delta));
            velocity = new Vec3d(velocity.x * 0.96, velocity.y - 0.008 * delta, velocity.z * 0.96);
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

