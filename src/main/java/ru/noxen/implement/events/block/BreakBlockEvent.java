package ru.noxen.implement.events.block;

import net.minecraft.util.math.BlockPos;
import ru.noxen.api.event.events.Event;

public record BreakBlockEvent(BlockPos blockPos) implements Event {}
