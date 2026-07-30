package ru.noxen.implement.features.modules.player;

import net.minecraft.item.Items;
import ru.noxen.api.event.EventHandler;
import ru.noxen.api.feature.module.Module;
import ru.noxen.api.feature.module.ModuleCategory;
import ru.noxen.common.QuickImports;
import ru.noxen.implement.events.player.TickEvent;
import ru.noxen.mixins.IMinecraftClientAccessor;

public class FastExp extends Module implements QuickImports {

    public static FastExp getInstance() {
        return ru.noxen.common.util.other.Instance.get(FastExp.class);
    }

    public FastExp() {
        super("FastExp", "Быстрый опыт", ModuleCategory.PLAYER);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null) return;
        if (mc.player.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE)
                || mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE)) {
            ((IMinecraftClientAccessor) mc).setItemUseCooldown(0);
        }
    }
}
