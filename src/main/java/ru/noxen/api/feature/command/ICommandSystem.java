package ru.noxen.api.feature.command;

import ru.noxen.api.feature.command.argparser.IArgParserManager;

public interface ICommandSystem {
    IArgParserManager getParserManager();
}
