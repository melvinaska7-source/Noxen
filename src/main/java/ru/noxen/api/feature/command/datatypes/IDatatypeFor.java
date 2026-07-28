package ru.noxen.api.feature.command.datatypes;

import ru.noxen.api.feature.command.exception.CommandException;

public interface IDatatypeFor<T> extends IDatatype  {
    T get(IDatatypeContext datatypeContext) throws CommandException;
}
