package ru.noxen.api.system.font.entry;

import ru.noxen.api.system.font.glyph.Glyph;

public record DrawEntry(float atX, float atY, int color, Glyph toDraw) {
}
