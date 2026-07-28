package ru.noxen.api.feature.command.datatypes;

import ru.noxen.api.feature.command.exception.CommandException;
import ru.noxen.common.QuickImports;

import java.util.stream.Stream;

public interface IDatatype extends QuickImports {
    Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException;
}
