package ru.noxen.implement.features.commands;

import ru.noxen.api.feature.command.ICommandSystem;
import ru.noxen.api.feature.command.argparser.IArgParserManager;
import ru.noxen.implement.features.commands.argparser.ArgParserManager;

public enum CommandSystem implements ICommandSystem {
    INSTANCE;

    @Override
    public IArgParserManager getParserManager() {
        return ArgParserManager.INSTANCE;
    }
}
