package ru.noxen.implement.events.block;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import ru.noxen.api.event.events.Event;

public record BlockBreakingEvent(BlockPos blockPos, Direction direction) implements Event {}
