package ru.noxen.implement.features.modules.render;

import ru.noxen.api.feature.module.Module;
import ru.noxen.api.feature.module.ModuleCategory;
import ru.noxen.api.feature.module.setting.implement.SelectSetting;
import ru.noxen.api.feature.module.setting.implement.ValueSetting;
import ru.noxen.common.QuickImports;
import ru.noxen.common.util.color.ColorUtil;
import ru.noxen.common.util.render.Render3DUtil;
import ru.noxen.implement.events.player.TickEvent;
import net.minecraft.util.math.Vec3d;
import ru.noxen.api.event.EventHandler;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Cubes extends Module implements QuickImports {

    public static Cubes getInstance() {
        return ru.noxen.common.util.other.Instance.get(Cubes.class);
    }

    public final SelectSetting shapeSetting = new SelectSetting("Shape", "Particle shape").value("Cube", "Triangle");
    public final ValueSetting countSetting = new ValueSetting("Count", "How many float around").range(3, 40).setValue(15);
    public final ValueSetting radiusSetting = new ValueSetting("Radius", "Spawn radius").range(1, 8).setValue(4);
    public final ValueSetting sizeSetting = new ValueSetting("Size", "Shape size").range(0.05f, 0.5f).setValue(0.18f);
    public final ValueSetting speedSetting = new ValueSetting("Speed", "Movement and rotation speed").range(0.05f, 2.0f).setValue(0.2f);

    private final CopyOnWriteArrayList<Cube> cubes = new CopyOnWriteArrayList<>();

    public Cubes() {
        super("Cubes", "Cubes", ModuleCategory.RENDER);
        setup(shapeSetting, countSetting, radiusSetting, sizeSetting, speedSetting);
    }

    @Override
    public void deactivate() {
        cubes.clear();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        int max = (int) countSetting.getValue();
        float radius = (float) radiusSetting.getValue();
        float speed = (float) speedSetting.getValue();

        while (cubes.size() < max) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            double angle = r.nextDouble() * Math.PI * 2;
            double dist = r.nextDouble() * radius;
            Vec3d spawnPos = mc.player.getPos().add(
                    Math.cos(angle) * dist,
                    r.nextDouble() * 3,
                    Math.sin(angle) * dist
            );
            cubes.add(new Cube(spawnPos, speed));
        }

        for (Cube c : cubes) c.tick(speed);
        cubes.removeIf(Cube::isDead);
    }

    @EventHandler
    public void onRender(ru.noxen.implement.events.render.WorldRenderEvent e) {
        float size = (float) sizeSetting.getValue();
        boolean triangle = shapeSetting.isSelected("Triangle");

        for (Cube c : cubes) {
            int color = ColorUtil.multAlpha(ColorUtil.fade((int) c.colorOffset), c.getAlpha());
            if (triangle) {
                drawTriangle(c.pos, size, c.rotX, c.rotY, color);
            } else {
                drawCube(c.pos, size, c.rotX, c.rotY, color);
            }
        }
    }

    private Vec3d rotate(double x, double y, double z, float rotX, float rotY) {
        // rotate around Y axis
        double cosY = Math.cos(rotY), sinY = Math.sin(rotY);
        double x1 = x * cosY - z * sinY;
        double z1 = x * sinY + z * cosY;
        // rotate around X axis
        double cosX = Math.cos(rotX), sinX = Math.sin(rotX);
        double y1 = y * cosX - z1 * sinX;
        double z2 = y * sinX + z1 * cosX;
        return new Vec3d(x1, y1, z2);
    }

    private void drawCube(Vec3d center, float size, float rotX, float rotY, int color) {
        double h = size / 2.0;
        Vec3d[] local = {
                new Vec3d(-h, -h, -h), new Vec3d(h, -h, -h), new Vec3d(h, h, -h), new Vec3d(-h, h, -h),
                new Vec3d(-h, -h, h), new Vec3d(h, -h, h), new Vec3d(h, h, h), new Vec3d(-h, h, h)
        };
        Vec3d[] world = new Vec3d[8];
        for (int i = 0; i < 8; i++) {
            Vec3d rotated = rotate(local[i].x, local[i].y, local[i].z, rotX, rotY);
            world[i] = center.add(rotated);
        }
        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0}, // bottom face
                {4, 5}, {5, 6}, {6, 7}, {7, 4}, // top face
                {0, 4}, {1, 5}, {2, 6}, {3, 7}  // verticals
        };
        for (int[] edge : edges) {
            Render3DUtil.drawLine(world[edge[0]], world[edge[1]], color, 1.5f, true);
        }
    }

    private void drawTriangle(Vec3d center, float size, float rotX, float rotY, int color) {
        double h = size / 2.0;
        Vec3d[] local = {
                new Vec3d(0, h, 0),
                new Vec3d(-h, -h, h * 0.6),
                new Vec3d(h, -h, h * 0.6)
        };
        Vec3d[] world = new Vec3d[3];
        for (int i = 0; i < 3; i++) {
            Vec3d rotated = rotate(local[i].x, local[i].y, local[i].z, rotX, rotY);
            world[i] = center.add(rotated);
        }
        Render3DUtil.drawLine(world[0], world[1], color, 1.5f, true);
        Render3DUtil.drawLine(world[1], world[2], color, 1.5f, true);
        Render3DUtil.drawLine(world[2], world[0], color, 1.5f, true);
    }

    private static class Cube {
        Vec3d pos;
        Vec3d velocity;
        float rotX, rotY;
        float rotSpeedX, rotSpeedY;
        float life;
        float maxLife;
        float colorOffset;

        Cube(Vec3d origin, float speed) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            this.pos = origin;
            this.velocity = new Vec3d(
                    (r.nextDouble() - 0.5) * 0.01 * speed,
                    (r.nextDouble() * 0.015 + 0.005) * speed,
                    (r.nextDouble() - 0.5) * 0.01 * speed
            );
            this.rotX = r.nextFloat() * 360;
            this.rotY = r.nextFloat() * 360;
            this.rotSpeedX = (r.nextFloat() * 0.6f + 0.2f) * speed;
            this.rotSpeedY = (r.nextFloat() * 0.6f + 0.2f) * speed;
            this.maxLife = r.nextFloat() * 200 + 200;
            this.life = maxLife;
            this.colorOffset = r.nextFloat() * 360;
        }

        void tick(float speed) {
            pos = pos.add(velocity);
            rotX += rotSpeedX;
            rotY += rotSpeedY;
            life -= 1;
        }

        float getAlpha() {
            float fadeIn = Math.min(1, (maxLife - life) / 20f);
            float fadeOut = Math.min(1, life / 20f);
            return Math.min(fadeIn, fadeOut);
        }

        boolean isDead() {
            return life <= 0;
        }
    }
}
