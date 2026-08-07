package ru.noxen.implement.features.modules.misc;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import ru.noxen.api.event.EventHandler;
import ru.noxen.api.feature.module.Module;
import ru.noxen.api.feature.module.ModuleCategory;
import ru.noxen.api.feature.module.setting.Setting;
import ru.noxen.api.feature.module.setting.implement.BooleanSetting;
import ru.noxen.api.feature.module.setting.implement.ColorSetting;
import ru.noxen.api.feature.module.setting.implement.GroupSetting;
import ru.noxen.api.feature.module.setting.implement.MultiSelectSetting;
import ru.noxen.common.util.color.ColorUtil;
import ru.noxen.common.util.entity.PlayerIntersectionUtil;
import ru.noxen.common.util.render.Render3DUtil;
import ru.noxen.implement.events.render.WorldRenderEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * RadiusHelper — при удержании кастомного FunTime-предмета в руке рисует зону его действия
 * (радиус/куб/плоскость/траекторию), и подсвечивает другим цветом, если в зоне есть игрок.
 *
 * Упрощено относительно референса: убрана система плавных цветовых fade-переходов
 * (в оригинале ~150 строк ради анимации) — здесь просто мгновенная смена цвета на "цвет
 * попадания", результат тот же самый визуально, но код короче и проще поддерживать.
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RadiusHelper extends Module {

    private enum Shape { CIRCLE, CUBE, PLANE, TRAJECTORY }

    private record ItemPreset(String label, Item item, Shape shape, double size, ColorSetting color) {}

    // ---- настройки цветов (дефолты как в референсе) ----
    ColorSetting dezkaColor = new ColorSetting("Цвет Дезки", "").value(0xFF005500);
    ColorSetting yavkaColor = new ColorSetting("Цвет Явки", "").value(0xFF999999);
    ColorSetting fireChargeColor = new ColorSetting("Цвет Огненного Заряда", "").value(0xFF550000);
    ColorSetting godAuraColor = new ColorSetting("Цвет Божьей Ауры", "").value(0xFF009999);
    ColorSetting trapkaColor = new ColorSetting("Цвет Трапки", "").value(0xFF8B4513);
    ColorSetting plastColor = new ColorSetting("Цвет Пласта", "").value(0xFF333333);
    ColorSetting snowballColor = new ColorSetting("Цвет Снежка", "").value(0xFFA0DCFF);

    List<ItemPreset> presets = new ArrayList<>();

    MultiSelectSetting itemsSetting = new MultiSelectSetting("Items", "Areas")
            .value("Дезка", "Явка", "Огненный заряд", "Божья аура", "Трапка", "Пласт", "Снежок");

    BooleanSetting hitIndicator = new BooleanSetting("Show hit", "Highlight an area if there is a player in it").setValue(true);
    ColorSetting hitColor = new ColorSetting("Hit Color", "").value(0xFF00FF88).visible(hitIndicator::isValue);
    BooleanSetting fillEnabled = new BooleanSetting("Fill", "Paint the area with a translucent color").setValue(true);

    GroupSetting colorsGroup = new GroupSetting("Items color", "Area color");

    public RadiusHelper() {
        super("FTRadiusHelper", "Shows the radius/area of effect of FunTime items", ModuleCategory.MISC);

        presets.add(new ItemPreset("Дезка", Items.ENDER_EYE, Shape.CIRCLE, 10.0, dezkaColor));
        presets.add(new ItemPreset("Явка", Items.SUGAR, Shape.CIRCLE, 10.0, yavkaColor));
        presets.add(new ItemPreset("Огненный заряд", Items.FIRE_CHARGE, Shape.CIRCLE, 10.0, fireChargeColor));
        presets.add(new ItemPreset("Божья аура", Items.PHANTOM_MEMBRANE, Shape.CIRCLE, 2.0, godAuraColor));
        presets.add(new ItemPreset("Трапка", Items.NETHERITE_SCRAP, Shape.CUBE, 4.0, trapkaColor));
        presets.add(new ItemPreset("Пласт", Items.DRIED_KELP, Shape.PLANE, 7.0, plastColor));
        presets.add(new ItemPreset("Снежок", Items.SNOWBALL, Shape.TRAJECTORY, 0, snowballColor));

        colorsGroup.settings(dezkaColor, yavkaColor, fireChargeColor, godAuraColor, trapkaColor, plastColor, snowballColor);
        setup(itemsSetting, colorsGroup, hitIndicator, hitColor, fillEnabled);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.player == null || mc.world == null) return;

        ItemPreset active = resolveActive(mc.player.getMainHandStack());
        if (active == null) active = resolveActive(mc.player.getOffHandStack());
        if (active == null) return;

        switch (active.shape()) {
            case CIRCLE -> renderCircle(active);
            case CUBE -> renderBox(active, new Box(-active.size() / 2, -1, -active.size() / 2, active.size() / 2, active.size() / 2, active.size() / 2)
                    .offset(mc.player.getPos()));
            case PLANE -> renderBox(active, new Box(-active.size() / 2, -0.05, -active.size() / 2, active.size() / 2, 0.05, active.size() / 2)
                    .offset(mc.player.getX(), mc.player.getY() - 1, mc.player.getZ()));
            case TRAJECTORY -> renderTrajectory(active);
        }
    }

    private ItemPreset resolveActive(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (ItemPreset preset : presets) {
            if (stack.getItem() == preset.item() && itemsSetting.isSelected(preset.label())) return preset;
        }
        return null;
    }

    private void renderCircle(ItemPreset preset) {
        Vec3d center = mc.player.getPos().add(0, -1.4, 0);
        boolean hit = hitIndicator.isValue() && playersNear(center, preset.size());
        int outline = colorFor(preset, hit);
        int fill = ColorUtil.multAlpha(outline, 0.35F);

        int steps = 72;
        Vec3d prev = null;
        for (int i = 0; i <= steps; i++) {
            double angle = Math.toRadians(i * (360.0 / steps));
            Vec3d point = center.add(Math.cos(angle) * preset.size(), 0, Math.sin(angle) * preset.size());
            if (prev != null) {
                Render3DUtil.drawLine(prev, point, outline, 2.5F, false);
                if (fillEnabled.isValue()) Render3DUtil.drawQuad(center, prev, point, center, fill, false);
            }
            prev = point;
        }
    }

    private void renderBox(ItemPreset preset, Box box) {
        boolean hit = hitIndicator.isValue() && !mc.world.getPlayers().stream()
                .filter(p -> p != mc.player)
                .filter(p -> box.intersects(p.getBoundingBox()))
                .toList().isEmpty();
        int outline = colorFor(preset, hit);
        int fill = ColorUtil.multAlpha(outline, 0.35F);
        Render3DUtil.drawBox(box, outline, 2.5F, true, fillEnabled.isValue(), false);
        if (fillEnabled.isValue()) Render3DUtil.drawBox(box, fill, 0, false, true, false);
    }

    private void renderTrajectory(ItemPreset preset) {
        Vec3d pos = mc.player.getEyePos();
        Vec3d velocity = mc.player.getRotationVector().multiply(1.5);
        int color = colorFor(preset, false);

        Vec3d prev = pos;
        for (int i = 0; i < 160; i++) {
            Vec3d next = prev.add(velocity);
            velocity = velocity.multiply(0.99).add(0, -0.03, 0);

            HitResult hit = mc.world.raycast(new RaycastContext(prev, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
            if (hit != null && hit.getType() != HitResult.Type.MISS) {
                Render3DUtil.drawLine(prev, hit.getPos(), color, 2.25F, false);
                Render3DUtil.drawBox(new Box(hit.getPos().add(-0.15, -0.15, -0.15), hit.getPos().add(0.15, 0.15, 0.15)), color, 2, true, true, false);
                return;
            }

            Render3DUtil.drawLine(prev, next, color, 2.25F, false);
            prev = next;
        }
    }

    private boolean playersNear(Vec3d center, double radius) {
        return PlayerIntersectionUtil.streamEntities()
                .filter(en -> en instanceof PlayerEntity && en != mc.player)
                .map(Entity::getPos)
                .anyMatch(p -> p.distanceTo(center) <= radius);
    }

    private int colorFor(ItemPreset preset, boolean hit) {
        return hit ? hitColor.getColor() : preset.color().getColor();
    }
}
