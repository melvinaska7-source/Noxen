package ru.noxen.implement.screens.menu.components.implement.other;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.noxen.api.system.font.Fonts;
import ru.noxen.api.system.shape.ShapeProperties;
import ru.noxen.common.util.color.ColorUtil;
import ru.noxen.common.util.render.Render2DUtil;
import ru.noxen.common.util.render.GlassPipeline;
import ru.noxen.implement.features.modules.render.Hud;
import ru.noxen.implement.screens.menu.MenuScreen;
import ru.noxen.implement.screens.menu.components.AbstractComponent;

import java.awt.Color;

@Setter
@Accessors(chain = true)
public class BackgroundComponent extends AbstractComponent {

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        if (Hud.getInstance().liquidGlassSetting.isValue()) {
            GlassPipeline.draw(matrix.peek().getPositionMatrix(), x, y, width, height, 6f, 6f, 3f, 0f, new Color(255, 255, 255, 35), 0.95f, 0f);
        } else {
            rectangle.render(ShapeProperties.create(matrix, x, y, width, height).round(6).softness(1).thickness(2).quality(50)
                    .outlineColor(ColorUtil.getOutline()).color(ColorUtil.getMainGuiColor()).build());
        }

        rectangle.render(ShapeProperties.create(context.getMatrices(), x + 85, y, 0.5F, height)
                .color(ColorUtil.getOutline(0.5F, 1)).build());
        rectangle.render(ShapeProperties.create(context.getMatrices(), x + 85.5F, y + 28, width - 85.5F, 0.5F)
                .color(ColorUtil.getOutline(0.5F, 1)).build());

        Fonts.getSize(16).drawString(matrix, MenuScreen.INSTANCE.getCategory().getReadableName(), x + 95, y + 13, 0xFFD4D6E1);
    }
}
