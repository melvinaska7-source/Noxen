package ru.noxen.implement.features.modules.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.noxen.api.feature.module.Module;
import ru.noxen.api.feature.module.ModuleCategory;
import ru.noxen.api.feature.module.setting.implement.MultiSelectSetting;
import ru.noxen.common.util.other.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class NoRender extends Module {
    public static NoRender getInstance() {
        return Instance.get(NoRender.class);
    }

    public MultiSelectSetting modeSetting = new MultiSelectSetting("Elements", "Select elements to be ignored")
            .value("Fire", "Bad Effects", "Block Overlay");

    public NoRender() {
        super("NoRender","No Render",ModuleCategory.RENDER);
        setup(modeSetting);
    }

}
