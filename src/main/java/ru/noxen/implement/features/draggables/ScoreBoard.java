package ru.noxen.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.*;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import ru.noxen.api.feature.draggable.AbstractDraggable;
import ru.noxen.api.system.font.FontRenderer;
import ru.noxen.api.system.font.Fonts;
import ru.noxen.api.system.shape.ShapeProperties;
import ru.noxen.common.util.color.ColorUtil;
import ru.noxen.common.util.render.GlassPipeline;
import ru.noxen.implement.features.modules.render.Hud;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ScoreBoard extends AbstractDraggable {
    private List<ScoreboardEntry> scoreboardEntries = new ArrayList<>();
    private ScoreboardObjective objective;

    public ScoreBoard() {
        super("Score Board", 10, 100, 100, 120,true);
    }

    @Override
    public boolean visible() {
        return !scoreboardEntries.isEmpty();
    }

    @Override
    public void tick() {
        objective = Objects.requireNonNull(mc.world).getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        scoreboardEntries = mc.world.getScoreboard().getScoreboardEntries(objective).stream().sorted(Comparator.comparing(ScoreboardEntry::value).reversed().thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(16);
        MutableText text = Text.empty();
        Text mainText = objective != null ? objective.getDisplayName() : Text.empty();

        scoreboardEntries.forEach(entry -> text.append(Team.decorateName(Objects.requireNonNull(mc.world).getScoreboard().getScoreHolderTeam(entry.owner()), entry.name())).append("\n"));

        int padding = 3;
        int offsetText = 14;
        int width = (int) Math.max(font.getStringWidth(text) + padding * 2 + 1,100);

        if (Hud.getInstance().liquidGlassSetting.isValue()) {
            GlassPipeline.draw(matrix.peek().getPositionMatrix(), getX(), getY(), getWidth(), offsetText, 4f, 5f, 3f, 0f, new Color(255, 255, 255, 45), 0.9f, 0f);
            GlassPipeline.draw(matrix.peek().getPositionMatrix(), getX(), getY() + offsetText - 0.5F, getWidth(), getHeight() - offsetText, 4f, 5f, 3f, 0f, new Color(255, 255, 255, 40), 0.9f, 0f);
        } else {
            blur.render(ShapeProperties.create(matrix,getX(),getY(),getWidth(),offsetText)
                    .round(4,0,4,0).thickness(2).softness(1).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRectDarker(0.9F)).build());
            blur.render(ShapeProperties.create(matrix,getX(),getY() + offsetText - 0.5F,getWidth(),getHeight() - offsetText)
                    .quality(40).round(0,4,0,4).thickness(2).softness(1).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());
        }

        font.drawText(matrix, mainText, (int) (getX() + (getWidth() - font.getStringWidth(mainText)) / 2),getY() + padding + 1.5F);
        font.drawText(matrix, text, getX() + padding,getY() + offsetText + padding);

        if (getX() > mc.getWindow().getScaledWidth() / 2) setX(getX() + getWidth() - width);
        setWidth(width);
        setHeight((int) (font.getStringHeight(text) / 2.16 + offsetText + padding));
    }
}
