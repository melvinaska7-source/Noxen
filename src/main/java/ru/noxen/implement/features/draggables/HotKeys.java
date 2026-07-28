package ru.noxen.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import ru.noxen.api.feature.draggable.AbstractDraggable;
import ru.noxen.api.feature.module.Module;
import ru.noxen.api.system.font.FontRenderer;
import ru.noxen.api.system.font.Fonts;
import ru.noxen.api.system.shape.ShapeProperties;
import ru.noxen.common.util.color.ColorUtil;
import ru.noxen.common.util.math.MathUtil;
import ru.noxen.common.util.other.StringUtil;
import ru.noxen.common.util.entity.PlayerIntersectionUtil;
import ru.noxen.core.Main;

import java.util.ArrayList;
import java.util.List;

public class HotKeys extends AbstractDraggable {
    private List<Module> keysList = new ArrayList<>();

    public HotKeys() {
        super("Hot Keys", 300, 10, 80, 23,true);
    }

    @Override
    public boolean visible() {
        return !keysList.isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        keysList = Main.getInstance().getModuleProvider().getModules().stream().filter(module -> module.getAnimation().getOutput().floatValue() != 0 && module.getKey() != -1).toList();
    }

    @Override
    public void drawDraggable(DrawContext e) {
        MatrixStack matrix = e.getMatrices();
        float centerX = getX() + getWidth() / 2F;

        FontRenderer font = Fonts.getSize(15, Fonts.Type.DEFAULT);
        FontRenderer fontModule = Fonts.getSize(13, Fonts.Type.DEFAULT);

        blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), 17.5F)
                .round(4,0,4,0).softness(1).thickness(2).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRectDarker(0.9F)).build());

        blur.render(ShapeProperties.create(matrix, getX(), getY() + 17, getWidth(), getHeight() - 17)
                .round(0,4,0,4).softness(1).thickness(2).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());

        font.drawString(matrix, getName(), (int) (centerX - font.getStringWidth(getName()) / 2), getY() + 7, ColorUtil.getText());

        int offset = 23;
        int maxWidth = 80;

        for (Module module : keysList) {
            String bind = "[" + StringUtil.getBindName(module.getKey()) + "]";
            float centerY = getY() + offset;
            float animation = module.getAnimation().getOutput().floatValue();
            float width = fontModule.getStringWidth(module.getName() + bind) + 15;

            MathUtil.scale(matrix,centerX,centerY,1,animation,() -> {
                fontModule.drawString(matrix, module.getName(), getX() + 6, centerY + 1, ColorUtil.getText());
                fontModule.drawString(matrix, bind, getX() + getWidth() - 6 - fontModule.getStringWidth(bind), centerY + 1, ColorUtil.getText());
            });

            offset += (int) (animation * 11);
            maxWidth = (int) Math.max(width, maxWidth);
        }

        setWidth(maxWidth);
        setHeight(offset);
    }
}
