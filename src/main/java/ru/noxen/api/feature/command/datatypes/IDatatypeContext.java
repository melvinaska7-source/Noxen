package ru.noxen.api.feature.command.datatypes;

import ru.noxen.api.feature.command.argument.IArgConsumer;

public interface IDatatypeContext {
    IArgConsumer getConsumer();
}
