package ru.noxen.common;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.stream.Stream;

public interface QuickLogger {
    static Text getPrefix() {
        int[] colors = {0x54DAF4, 0x54BBE5, 0x549CD5, 0x547DC6, 0x545EB6};
        String letters = "NOXEN";

        MutableText text = Text.literal("[").setStyle(Text.empty().getStyle().withColor(Formatting.WHITE));
        for (int i = 0; i < letters.length(); i++) {
            MutableText letter = Text.literal(String.valueOf(letters.charAt(i)));
            letter.setStyle(letter.getStyle()
                    .withColor(net.minecraft.text.TextColor.fromRgb(colors[i]))
                    .withBold(true));
            text.append(letter);
        }
        text.append(Text.literal("]").setStyle(Text.empty().getStyle().withColor(Formatting.WHITE)));
        text.append(" -> ");
        return text;
    }

    default void logDirect(Text... components) {
        MutableText component = Text.literal("");
        component.append(getPrefix());
        component.append(Text.literal(" "));
        Arrays.asList(components).forEach(component::append);
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(component);
        }
    }

    default void logDirect(String message, Formatting color) {
        Stream.of(message.split("\n")).forEach(line -> {
            MutableText component = Text.literal(line.replace("\t", "    "));
            component.setStyle(component.getStyle().withColor(color));
            logDirect(component);
        });
    }

    default void logDirect(String message) {
        logDirect(message, Formatting.GRAY);
    }
}
