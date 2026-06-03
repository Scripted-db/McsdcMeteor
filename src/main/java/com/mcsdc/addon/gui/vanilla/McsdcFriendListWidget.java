package com.mcsdc.addon.gui.vanilla;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class McsdcFriendListWidget extends ClickableWidget {
    public static final int ROW_HEIGHT = 20;

    public record Row(String name, String col2, String col3) {}

    private List<Row> rows = List.of();
    private int selected = -1;
    private double scrollY;

    public McsdcFriendListWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Text.empty());
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
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        ctx.fill(x, y, x + w, y + h, 0xC0101010);
        ctx.enableScissor(x, y, x + w, y + h);

        var tr = MinecraftClient.getInstance().textRenderer;
        for (int i = 0; i < rows.size(); i++) {
            int rowY = y + 1 + i * ROW_HEIGHT - (int) scrollY;
            if (rowY + ROW_HEIGHT < y) continue;
            if (rowY > y + h) break;

            Row row = rows.get(i);
            boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            boolean sel = i == selected;

            if (sel) ctx.fill(x, rowY, x + w, rowY + ROW_HEIGHT, 0x80808080);
            else if (hovered) ctx.fill(x, rowY, x + w, rowY + ROW_HEIGHT, 0x40404040);

            int color = sel || hovered ? Colors.WHITE : Colors.LIGHT_GRAY;
            ctx.drawTextWithShadow(tr, row.name(), x + 4, rowY + 5, color);
            ctx.drawTextWithShadow(tr, row.col2(), x + 120, rowY + 5, Colors.LIGHT_GRAY);
            if (!row.col3().isEmpty()) {
                ctx.drawTextWithShadow(tr, row.col3(), x + 220, rowY + 5, Colors.GRAY);
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
    public boolean mouseClicked(Click click, boolean doubled) {
        if (!active || !visible) return false;
        if (!isMouseOver(click.x(), click.y())) return false;

        int idx = ((int) click.y() - getY() + (int) scrollY) / ROW_HEIGHT;
        if (idx >= 0 && idx < rows.size()) {
            selected = idx;
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        int max = Math.max(0, rows.size() * ROW_HEIGHT - getHeight());
        scrollY = Math.clamp(scrollY - vertical * ROW_HEIGHT, 0, max);
        return true;
    }
}
