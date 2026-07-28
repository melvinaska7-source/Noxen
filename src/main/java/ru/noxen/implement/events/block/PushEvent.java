package ru.noxen.implement.events.block;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.noxen.api.event.events.callables.EventCancellable;

@Getter
@AllArgsConstructor
public class PushEvent extends EventCancellable {
    private Type type;

    public enum Type {
        COLLISION, BLOCK, WATER
    }
}
