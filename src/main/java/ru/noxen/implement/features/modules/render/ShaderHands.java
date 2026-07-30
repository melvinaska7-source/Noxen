package ru.noxen.implement.features.modules.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import ru.noxen.api.event.EventHandler;
import ru.noxen.api.feature.module.Module;
import ru.noxen.api.feature.module.ModuleCategory;
import ru.noxen.api.feature.module.setting.implement.ValueSetting;
import ru.noxen.common.QuickImports;
import ru.noxen.implement.events.item.HandOffsetEvent;

public class ShaderHands extends Module implements QuickImports {

    public static ShaderHands getInstance() {
        return ru.noxen.common.util.other.Instance.get(ShaderHands.class);
    }

    public final ValueSetting waveSpeedSetting = new ValueSetting("Скорость волны", "Как быстро колышется рука").range(0.5f, 8f).setValue(3f);
    public final ValueSetting waveScaleSetting = new ValueSetting("Сила волны", "Насколько сильно отклоняется рука").range(0.01f, 0.3f).setValue(0.08f);

    public ShaderHands() {
        super("ShaderHands", "Волна руки", ModuleCategory.RENDER);
        setup(waveSpeedSetting, waveScaleSetting);
    }

    @EventHandler
    public void onHandOffset(HandOffsetEvent e) {
        if (e.getHand() != Hand.MAIN_HAND) return;

        float speed = (float) waveSpeedSetting.getValue();
        float scale = (float) waveScaleSetting.getValue();

        long time = System.currentTimeMillis();
        float t = time / 1000f * speed;

        float offsetY = (float) Math.sin(t) * scale;
        float offsetX = (float) Math.sin(t * 0.7f) * scale * 0.5f;
        float rotZ = (float) Math.sin(t * 0.9f) * scale * 15f;

        MatrixStack matrix = e.getMatrices();
        matrix.translate(offsetX, offsetY, 0);
        matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotZ));
    }
}
