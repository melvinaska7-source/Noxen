package ru.noxen.implement.features.modules.misc;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import ru.noxen.api.event.EventHandler;
import ru.noxen.api.feature.module.Module;
import ru.noxen.api.feature.module.ModuleCategory;
import ru.noxen.api.feature.module.setting.Setting;
import ru.noxen.api.feature.module.setting.implement.BindSetting;
import ru.noxen.api.feature.module.setting.implement.GroupSetting;
import ru.noxen.common.util.entity.PlayerInventoryUtil;
import ru.noxen.implement.events.keyboard.KeyEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FTHelper — использует кастомные предметы FunTime по нажатию привязанной клавиши.
 * Предмет ищется по вхождению названия в имя стака (лор/кастом-нейм сервера),
 * с проверкой кулдауна базового ванильного предмета, на который "скинен" кастомный.
 *
 * Все бинды свёрнуты в один GroupSetting ("Кнопки предметов") — чтобы не раздувать
 * высоту модуля в общем списке настроек и не упираться в баг скролла CategoryComponent
 * (он не рассчитан на модули с большим количеством строк-настроек).
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FTHelper extends Module {

    private record ThrowableItem(String label, String search, Item base) {}

    List<ThrowableItem> items = List.of(
            new ThrowableItem("Дезориентация", "дезориентация", Items.ENDER_EYE),
            new ThrowableItem("Божья аура", "божья аура", Items.PHANTOM_MEMBRANE),
            new ThrowableItem("Хлопушка", "хлопушка", Items.SPLASH_POTION),
            new ThrowableItem("Святая вода", "святая вода", Items.SPLASH_POTION),
            new ThrowableItem("Зелье гнева", "зелье гнева", Items.SPLASH_POTION),
            new ThrowableItem("Зелье паладина", "зелье паладина", Items.SPLASH_POTION),
            new ThrowableItem("Зелье ассасина", "зелье ассасина", Items.SPLASH_POTION),
            new ThrowableItem("Зелье радиации", "зелье радиации", Items.SPLASH_POTION),
            new ThrowableItem("Снотворное", "снотворное", Items.SPLASH_POTION),
            new ThrowableItem("Шалкер", "ящик", Items.SHULKER_BOX),
            new ThrowableItem("Трапка", "трапка", Items.NETHERITE_SCRAP),
            new ThrowableItem("Огненный смерч", "огненный смерч", Items.FIRE_CHARGE),
            new ThrowableItem("Арбалет", "арбалет", Items.CROSSBOW),
            new ThrowableItem("Пласт", "пласт", Items.DRIED_KELP),
            new ThrowableItem("Явная пыль", "явная пыль", Items.SUGAR),
            new ThrowableItem("Заморозка", "заморозка", Items.SNOWBALL)
    );

    Map<ThrowableItem, BindSetting> binds = new LinkedHashMap<>();
    GroupSetting bindsGroup = new GroupSetting("Кнопки предметов", "Настройка кнопок для каждого предмета FunTime");

    public FTHelper() {
        super("FTHelper", "Использует предметы FunTime по кнопкам биндов", ModuleCategory.MISC);

        Setting[] settings = new Setting[items.size()];
        for (int i = 0; i < items.size(); i++) {
            ThrowableItem item = items.get(i);
            BindSetting bind = new BindSetting(item.label(), "Использовать \"" + item.label() + "\" по нажатию");
            binds.put(item, bind);
            settings[i] = bind;
        }
        bindsGroup.settings(settings);
        setup(bindsGroup);
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        for (Map.Entry<ThrowableItem, BindSetting> entry : binds.entrySet()) {
            if (e.isKeyDown(entry.getValue().getKey())) use(entry.getKey());
        }
    }

    private void use(ThrowableItem item) {
        if (mc.player == null) return;
        if (mc.player.getItemCooldownManager().isCoolingDown(item.base().getDefaultStack())) return;

        Slot slot = PlayerInventoryUtil.getSlot(s ->
                s.getStack().getItem().equals(item.base())
                        && s.getStack().getName().getString().toLowerCase().contains(item.search()));

        PlayerInventoryUtil.swapAndUse(slot, item.label(), true);
    }
}
