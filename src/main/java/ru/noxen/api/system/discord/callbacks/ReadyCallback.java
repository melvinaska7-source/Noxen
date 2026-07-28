package ru.noxen.api.system.discord.callbacks;

import com.sun.jna.Callback;
import ru.noxen.api.system.discord.utils.DiscordUser;

public interface ReadyCallback extends Callback {
    void apply(DiscordUser var1);
}