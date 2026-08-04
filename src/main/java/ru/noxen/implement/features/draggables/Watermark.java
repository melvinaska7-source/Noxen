package ru.noxen.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import ru.noxen.api.feature.draggable.AbstractDraggable;
import ru.noxen.api.system.font.FontRenderer;
import ru.noxen.api.system.font.Fonts;
import ru.noxen.api.system.shape.ShapeProperties;
import ru.noxen.common.util.color.ColorUtil;
import ru.noxen.common.util.math.MathUtil;
import ru.noxen.common.util.other.StringUtil;
import ru.noxen.common.util.render.GlassPipeline;
import ru.noxen.core.Main;
import ru.noxen.implement.features.modules.render.Hud;

import java.awt.Color;

public class Watermark extends AbstractDraggable {
    private int fpsCount = 0;

    public Watermark() {
        super("Watermark", 10, 10, 92, 16,true);
    }

    @Override
    public void tick() {
        fpsCount = (int) MathUtil.interpolate(fpsCount, mc.getCurrentFps());
    }

    @Override
    public void drawDraggable(DrawContext e) {
        MatrixStack matrix = e.getMatrices();
        FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);

        String offset = "      ";
        String name = Main.getInstance().getClientInfoProvider().clientName() + offset;
        String version = StringUtil.getUserRole() + offset;
        String fps = fpsCount + " FPS";

        if (Hud.getInstance().liquidGlassSetting.isValue()) {
            GlassPipeline.draw(matrix.peek().getPositionMatrix(), getX(), getY(), getWidth(), getHeight(), 3f, 5f, 3f, 0f, new Color(255, 255, 255, 40), 0.9f, 0f);
        } else {
            blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                    .round(3).softness(1).thickness(2).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());
        }
        font.drawGradientString(matrix, name, getX() + 5, getY() + 6.5F, ColorUtil.fade(0), ColorUtil.fade(100));
        font.drawString(matrix, version + fps, getX() + font.getStringWidth(name) + 5, getY() + 6.5F, ColorUtil.getText());
        rectangle.render(ShapeProperties.create(matrix, getX() + font.getStringWidth(name),getY() + 4,0.5F,getHeight() - 8).color(ColorUtil.getOutline(0.75F,0.5f)).build());
        rectangle.render(ShapeProperties.create(matrix, getX() + font.getStringWidth(name + version),getY() + 4,0.5F,getHeight() - 8).color(ColorUtil.getOutline(0.75F,0.5f)).build());
        setWidth((int) (font.getStringWidth(name + version + fps) + 9));
    }
}
