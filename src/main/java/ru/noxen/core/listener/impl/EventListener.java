package ru.noxen.core.listener.impl;

import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import ru.noxen.api.event.EventHandler;
import ru.noxen.api.feature.draggable.AbstractDraggable;
import ru.noxen.common.util.entity.PlayerInventoryComponent;
import ru.noxen.common.util.world.ServerUtil;
import ru.noxen.core.Main;
import ru.noxen.core.listener.Listener;
import ru.noxen.implement.events.packet.PacketEvent;
import ru.noxen.implement.events.player.TickEvent;

public class EventListener implements Listener {
    public static boolean serverSprint;
    public static int selectedSlot;

    @EventHandler
    public void onTick(TickEvent e) {
        ServerUtil.tick();
        PlayerInventoryComponent.tick();
        Main.getInstance().getDraggableRepository().draggable().forEach(AbstractDraggable::tick);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        switch (e.getPacket()) {
            case ClientCommandC2SPacket command -> serverSprint = switch (command.getMode()) {
                case ClientCommandC2SPacket.Mode.START_SPRINTING -> true;
                case ClientCommandC2SPacket.Mode.STOP_SPRINTING -> false;
                default -> serverSprint;
            };
            case UpdateSelectedSlotC2SPacket slot -> selectedSlot = slot.getSelectedSlot();
            default -> {}
        }
        ServerUtil.packet(e);
        Main.getInstance().getDraggableRepository().draggable().forEach(drag -> drag.packet(e));
    }

}
