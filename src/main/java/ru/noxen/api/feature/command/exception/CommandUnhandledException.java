package ru.noxen.api.feature.command.exception;

import ru.noxen.api.feature.command.ICommand;
import ru.noxen.api.feature.command.argument.ICommandArgument;
import ru.noxen.common.QuickLogger;

import java.util.List;

public class CommandUnhandledException extends RuntimeException implements ICommandException, QuickLogger {

    public CommandUnhandledException(String message) {
        super(message);
    }

    public CommandUnhandledException(Throwable cause) {
        super(cause);
    }

    @Override
    public void handle(ICommand command, List<ICommandArgument> args) {
    }
}
