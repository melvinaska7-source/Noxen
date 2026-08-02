package ru.noxen.implement.features.modules.combat;

import ru.noxen.api.event.EventHandler;
import ru.noxen.api.feature.module.Module;
import ru.noxen.api.feature.module.ModuleCategory;
import ru.noxen.api.feature.module.setting.implement.ValueSetting;
import ru.noxen.common.QuickImports;
import ru.noxen.implement.events.player.AttackEvent;
import ru.noxen.implement.events.player.TickEvent;

public class ShiftTap extends Module implements QuickImports {

    public static ShiftTap getInstance() {
        return ru.noxen.common.util.other.Instance.get(ShiftTap.class);
    }

    public final ValueSetting durationSetting = new ValueSetting("Duration", "How long to hold sneak after an attack (ms)").range(10, 200).setValue(25);

    private long endTime = 0;
    private boolean controllingSneak = false;

    public ShiftTap() {
        super("ShiftTap", "ShiftTap", ModuleCategory.COMBAT);
        setup(durationSetting);
    }

    @Override
    public void deactivate() {
        stop();
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        if (mc.player == null) return;
        endTime = System.currentTimeMillis() + (long) durationSetting.getValue();
        if (!controllingSneak) {
            mc.options.sneakKey.setPressed(true);
            controllingSneak = true;
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.player.isSpectator()) {
            stop();
            return;
        }
        if (controllingSneak && System.currentTimeMillis() > endTime) {
            stop();
        }
    }

    private void stop() {
        if (controllingSneak) {
            mc.options.sneakKey.setPressed(false);
            controllingSneak = false;
        }
    }
}
