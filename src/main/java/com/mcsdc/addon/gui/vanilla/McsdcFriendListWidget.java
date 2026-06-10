package com.mcsdc.addon.gui.vanilla;

import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class McsdcFriendListWidget extends AbstractWidget {
    public static final int ROW_HEIGHT = 20;

    public record Row(String name, String col2, String col3) {}

    private List<Row> rows = List.of();
    private int selected = -1;
    private double scrollY;

    public McsdcFriendListWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    public void setRows(List<Row> rows) {
        this.rows = List.copyOf(rows);
        this.selected = -1;
        this.scrollY = 0;
    }

    @Nullable
    public Row getSelectedRow() {
        if (selected < 0 || selected >= rows.size()) return null;
        return rows.get(selected);
    }

    @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        ctx.fill(x, y, x + w, y + h, 0xC0101010);
        ctx.enableScissor(x, y, x + w, y + h);

        var tr = Minecraft.getInstance().font;
        for (int i = 0; i < rows.size(); i++) {
            int rowY = y + 1 + i * ROW_HEIGHT - (int) scrollY;
            if (rowY + ROW_HEIGHT < y) continue;
            if (rowY > y + h) break;

            Row row = rows.get(i);
            boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            boolean sel = i == selected;

            if (sel) ctx.fill(x, rowY, x + w, rowY + ROW_HEIGHT, 0x80808080);
            else if (hovered) ctx.fill(x, rowY, x + w, rowY + ROW_HEIGHT, 0x40404040);

            int color = sel || hovered ? CommonColors.WHITE : CommonColors.LIGHT_GRAY;
            ctx.text(tr, row.name(), x + 4, rowY + 5, color, true);
            ctx.text(tr, row.col2(), x + 120, rowY + 5, CommonColors.LIGHT_GRAY, true);
            if (!row.col3().isEmpty()) {
                ctx.text(tr, row.col3(), x + 220, rowY + 5, CommonColors.GRAY, true);
            }
        }

        ctx.disableScissor();

        int contentH = rows.size() * ROW_HEIGHT;
        if (contentH > h) {
            int maxScroll = contentH - h;
            int barX = x + w - 6;
            int thumbH = Math.max(16, h * h / contentH);
            int thumbY = y + (int) ((scrollY / maxScroll) * (h - thumbH));
            ctx.fill(barX, y, barX + 5, y + h, 0x40FFFFFF);
            ctx.fill(barX, thumbY, barX + 5, thumbY + thumbH, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!active || !visible) return false;
        if (!isMouseOver(event.x(), event.y())) return false;

        int idx = ((int) event.y() - getY() + (int) scrollY) / ROW_HEIGHT;
        if (idx >= 0 && idx < rows.size()) {
            selected = idx;
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        defaultButtonNarrationText(builder);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        int max = Math.max(0, rows.size() * ROW_HEIGHT - getHeight());
        scrollY = Math.clamp(scrollY - vertical * ROW_HEIGHT, 0, max);
        return true;
    }
}
