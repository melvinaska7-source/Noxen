package ru.noxen.common.trait;

import ru.noxen.api.feature.module.setting.Setting;

public interface Setupable {
    void setup(Setting... settings);
}