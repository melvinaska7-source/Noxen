package ru.noxen.implement.events.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.noxen.api.event.events.Event;

@Getter
@AllArgsConstructor
public class RotationUpdateEvent implements Event {
    byte type;
}
