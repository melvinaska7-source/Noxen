package ru.noxen.api.feature.command.exception;

import net.minecraft.util.Formatting;
import ru.noxen.api.feature.command.ICommand;
import ru.noxen.api.feature.command.argument.ICommandArgument;
import ru.noxen.common.QuickLogger;

import java.util.List;

public interface ICommandException extends QuickLogger {

    String getMessage();

    default void handle(ICommand command, List<ICommandArgument> args) {
        logDirect(
                this.getMessage(),
                Formatting.RED
        );
    }
}
