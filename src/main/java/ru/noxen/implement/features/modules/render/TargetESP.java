package ru.noxen.implement.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import ru.noxen.api.event.EventHandler;
import ru.noxen.api.feature.module.Module;
import ru.noxen.api.feature.module.ModuleCategory;
import ru.noxen.api.feature.module.setting.implement.BooleanSetting;
import ru.noxen.api.feature.module.setting.implement.ColorSetting;
import ru.noxen.api.feature.module.setting.implement.SelectSetting;
import ru.noxen.api.feature.module.setting.implement.ValueSetting;
import ru.noxen.common.util.color.ColorUtil;
import ru.noxen.common.util.math.MathUtil;
import ru.noxen.common.util.math.ProjectionUtil;
import ru.noxen.common.util.other.Instance;
import ru.noxen.common.util.render.Render2DUtil;
import ru.noxen.implement.events.render.DrawEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TargetESP extends Module {

    public static TargetESP getInstance() {
        return Instance.get(TargetESP.class);
    }

    SelectSetting type = new SelectSetting("Type", "Target ESP mode")
            .value("BoyKisser", "Round", "Default", "Star", "Sword", "Saw", "Rounded", "Ghosts", "Pro");

    ValueSetting sizeSetting = new ValueSetting("Size", "Image size")
            .setValue(70).range(20, 160);

    ValueSetting speedSetting = new ValueSetting("Speed", "Rotation / ghost speed")
            .setValue(2.0f).range(0.5f, 9.0f);

    ValueSetting brightSetting = new ValueSetting("Brightness", "Ghost trail brightness")
            .setValue(220).range(50, 255)
            .visible(() -> type.isSelected("Ghosts"));

    BooleanSetting useClientColor = new BooleanSetting("Client Color", "Use HUD client color")
            .setValue(true);

    ColorSetting colorSetting = new ColorSetting("Color", "Custom color")
            .setColor(0xFFFF69B4)
            .presets(0xFFFF69B4, 0xFF6C9AFD, 0xFF8C7FFF, 0xFFFF7B7B)
            .visible(() -> !useClientColor.isValue());

    BooleanSetting players = new BooleanSetting("Players", "Show on players").setValue(true);
    BooleanSetting mobs = new BooleanSetting("Mobs", "Show on mobs").setValue(true);
    BooleanSetting animals = new BooleanSetting("Animals", "Show on animals").setValue(true);

    public TargetESP() {
        super("TargetESP", "TargetESP", ModuleCategory.RENDER);
        setup(type, sizeSetting, speedSetting, brightSetting, useClientColor, colorSetting, players, mobs, animals);
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.ENTITY) return;

        Entity entity = ((EntityHitResult) mc.crosshairTarget).getEntity();
        if (!(entity instanceof LivingEntity living) || living == mc.player) return;
        if (living instanceof ArmorStandEntity) return;
        if (!isValidTarget(living)) return;

        float partialTicks = e.getPartialTicks();
        Vec3d interpolated = MathUtil.interpolate(living);
        Vec3d center = interpolated.add(0, living.getHeight() * 0.5, 0);

        int baseColor = useClientColor.isValue() ? ColorUtil.getClientColor() : colorSetting.getColor();
        if (living.hurtTime > 0) {
            float mul = 0.55f + 0.45f * (living.hurtTime / 10f);
            baseColor = ColorUtil.multAlpha(baseColor, mul);
        }

        if (type.isSelected("Ghosts")) {
            renderGhosts(e, living, interpolated, baseColor);
            return;
        }

        Vec3d screen = ProjectionUtil.worldSpaceToScreenSpace(center);
        if (screen.z <= 0 || screen.z >= 1) return;

        Identifier texture = getTexture();
        if (texture == null) return;

        float size = sizeSetting.getValue();
        float half = size / 2f;
        float x = (float) screen.x;
        float y = (float) screen.y;

        DrawContext context = e.getDrawContext();
        MatrixStack matrices = context.getMatrices();

        matrices.push();
        matrices.translate(x, y, 0);
        float angle = (System.currentTimeMillis() % 360000L) / 1000f * speedSetting.getValue() * 40f;
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
        matrices.translate(-half, -half, 0);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        Render2DUtil.drawTexture(
                matrices,
                texture,
                0, 0,
                size, size,
                0, 0,
                64, 64,
                64, 64,
                baseColor
        );

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private void renderGhosts(DrawEvent e, LivingEntity living, Vec3d interpolated, int baseColor) {
        float speed = speedSetting.getValue();
        float size = sizeSetting.getValue();
        int brightness = (int) brightSetting.getValue();
        double time = System.currentTimeMillis() / (500.0 / speed);

        Identifier glow = Identifier.of("textures/glow.png");
        Vec3d bodyPos = interpolated.add(0, living.getHeight() * 0.5, 0);

        DrawContext context = e.getDrawContext();
        MatrixStack matrices = context.getMatrices();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        for (int j = 0; j < 30; j++) {
            float alpha = brightness - j * 6f;
            if (alpha <= 0) continue;

            float trailSize = size * (1f - j * 0.025f);
            double trailTime = time - j * 0.1;
            double trailSin = Math.sin(trailTime);
            double trailCos = Math.cos(trailTime);
            float angleOffset = j * 8f;

            // верхняя орбита
            Vec3d pos3d = bodyPos.add(trailCos * 0.55, Math.sin(trailTime) * 0.25, trailSin * 0.55);
            Vec3d screen = ProjectionUtil.worldSpaceToScreenSpace(pos3d);
            if (screen.z > 0 && screen.z < 1) {
                int col = ColorUtil.multAlpha(baseColor, alpha / 255f);
                drawGhostQuad(matrices, glow, (float) screen.x, (float) screen.y, trailSize, (float) (trailSin * 360 + angleOffset), col);
            }

            // нижняя орбита
            pos3d = bodyPos.add(-trailCos * 0.55, Math.sin(trailTime) * 0.15 - 0.2, -trailSin * 0.55);
            screen = ProjectionUtil.worldSpaceToScreenSpace(pos3d);
            if (screen.z > 0 && screen.z < 1) {
                int col = ColorUtil.multAlpha(baseColor, alpha / 255f);
                drawGhostQuad(matrices, glow, (float) screen.x, (float) screen.y, trailSize, (float) (-trailSin * 360 + angleOffset), col);
            }
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private void drawGhostQuad(MatrixStack matrices, Identifier texture, float x, float y, float size, float angle, int color) {
        float half = size / 2f;
        matrices.push();
        matrices.translate(x, y, 0);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
        matrices.translate(-half, -half, 0);
        Render2DUtil.drawTexture(matrices, texture, 0, 0, size, size, 0, 0, 64, 64, 64, 64, color);
        matrices.pop();
    }

    private Identifier getTexture() {
        if (type.isSelected("BoyKisser")) return Identifier.of("textures/boykisser.png");
        if (type.isSelected("Round")) return Identifier.of("textures/target1.png");
        if (type.isSelected("Default")) return Identifier.of("textures/target.png");
        if (type.isSelected("Star")) return Identifier.of("textures/star.png");
        if (type.isSelected("Sword")) return Identifier.of("textures/mech.png");
        if (type.isSelected("Saw")) return Identifier.of("textures/pila.png");
        if (type.isSelected("Rounded")) return Identifier.of("textures/zxcvbn.png");
        if (type.isSelected("Pro")) return Identifier.of("textures/targetpro.png");
        return null;
    }

    private boolean isValidTarget(LivingEntity e) {
        if (e instanceof PlayerEntity) return players.isValue();
        if (e.getType().getSpawnGroup().isPeaceful()) {
            return animals.isValue() || mobs.isValue();
        }
        return mobs.isValue();
    }
}
