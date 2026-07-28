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

    Identifier boykisserId = Identifier.of("textures/boykisser.png");

    ValueSetting sizeSetting = new ValueSetting("Size", "Size of the image")
            .setValue(70).range(30, 150);

    ValueSetting speedSetting = new ValueSetting("Speed", "Rotation speed")
            .setValue(2.0f).range(0.5f, 8.0f);

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
        super("TargetESP", "Target ESP", ModuleCategory.RENDER);
        setup(sizeSetting, speedSetting, useClientColor, colorSetting, players, mobs, animals);
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

        Vec3d screen = ProjectionUtil.worldSpaceToScreenSpace(center);
        if (screen.z <= 0 || screen.z >= 1) return;

        float size = sizeSetting.getValue();
        float half = size / 2f;
        float x = (float) screen.x;
        float y = (float) screen.y;

        int color = useClientColor.isValue()
                ? ColorUtil.getClientColor()
                : colorSetting.getColor();

        if (living.hurtTime > 0) {
            float alphaMul = 0.55f + 0.45f * (living.hurtTime / 10f);
            color = ColorUtil.multAlpha(color, alphaMul);
        }

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
                boykisserId,
                0, 0,
                size, size,
                0, 0,
                64, 64,
                64, 64,
                color
        );

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private boolean isValidTarget(LivingEntity e) {
        if (e instanceof PlayerEntity) return players.isValue();
        if (e.getType().getSpawnGroup().isPeaceful()) {
            return animals.isValue() || mobs.isValue();
        }
        return mobs.isValue();
    }
}
