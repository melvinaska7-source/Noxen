package ru.noxen.implement.events.item;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.noxen.api.event.events.callables.EventCancellable;

@Getter
@Setter
@AllArgsConstructor
public class UsingItemEvent extends EventCancellable {
    byte type;
}
