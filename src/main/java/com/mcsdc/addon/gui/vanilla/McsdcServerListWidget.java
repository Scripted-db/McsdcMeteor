package com.mcsdc.addon.gui.vanilla;

import com.mcsdc.addon.system.ServerStorage;
import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class McsdcServerListWidget extends AbstractWidget {
    public static final int ROW_HEIGHT = 18;

    private List<ServerStorage> servers = List.of();
    private int selected = -1;
    private double scrollY;
    private boolean draggingScrollbar;
    private double scrollbarDragOffset;
    @Nullable private Runnable onSelectionChanged;

    public McsdcServerListWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    public void setOnSelectionChanged(@Nullable Runnable onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
    }

    public void setServers(List<ServerStorage> servers) {
        this.servers = List.copyOf(servers);
        this.selected = -1;
        this.scrollY = 0;
        this.draggingScrollbar = false;
        notifySelectionChanged();
    }

    @Nullable
    public ServerStorage getSelectedServer() {
        if (selected < 0 || selected >= servers.size()) return null;
        return servers.get(selected);
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
        for (int i = 0; i < servers.size(); i++) {
            int rowY = y + 1 + i * ROW_HEIGHT - (int) scrollY;
            if (rowY + ROW_HEIGHT < y) continue;
            if (rowY > y + h) break;

            ServerStorage server = servers.get(i);
            boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            boolean sel = i == selected;

            if (sel) ctx.fill(x, rowY, x + w, rowY + ROW_HEIGHT, 0x80808080);
            else if (hovered) ctx.fill(x, rowY, x + w, rowY + ROW_HEIGHT, 0x40404040);

            int color = sel || hovered ? CommonColors.WHITE : CommonColors.LIGHT_GRAY;
            ctx.text(tr, server.ip(), x + 4, rowY + 4, color, true);
            String version = server.version() != null ? server.version() : "?";
            ctx.text(tr, version, x + w / 2, rowY + 4, color, true);
        }

        ctx.disableScissor();
        drawScrollbar(ctx, x, y, w, h);
    }

    private void drawScrollbar(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
        if (!hasScrollbar()) return;
        int barX = scrollbarX();
        int thumbH = scrollbarThumbHeight();
        int thumbY = scrollbarThumbY();
        ctx.fill(barX, y, barX + 5, y + h, 0x40FFFFFF);
        ctx.fill(barX, thumbY, barX + 5, thumbY + thumbH, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!active || !visible) return false;
        if (!isMouseOver(event.x(), event.y())) return false;

        if (hasScrollbar() && isMouseOverScrollbar(event.x(), event.y())) {
            draggingScrollbar = true;
            scrollbarDragOffset = event.y() - scrollbarThumbY();
            setScrollFromScrollbarY(event.y() - scrollbarDragOffset);
            return true;
        }

        if (selectIndex(indexAtClick(event.y()))) return true;
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (!draggingScrollbar) return false;
        setScrollFromScrollbarY(event.y() - scrollbarDragOffset);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!draggingScrollbar) return false;
        draggingScrollbar = false;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        scrollY = Math.clamp(scrollY - vertical * ROW_HEIGHT, 0, maxScroll());
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        defaultButtonNarrationText(builder);
    }

    private int contentHeight() {
        return servers.size() * ROW_HEIGHT;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - getHeight());
    }

    private boolean hasScrollbar() {
        return contentHeight() > getHeight();
    }

    private int scrollbarX() {
        return getX() + getWidth() - 6;
    }

    private int scrollbarThumbHeight() {
        return Math.max(16, getHeight() * getHeight() / contentHeight());
    }

    private int scrollbarThumbY() {
        int trackH = getHeight() - scrollbarThumbHeight();
        return getY() + (int) ((scrollY / maxScroll()) * trackH);
    }

    private boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        int barX = scrollbarX();
        int thumbY = scrollbarThumbY();
        int thumbH = scrollbarThumbHeight();
        return mouseX >= barX && mouseX < barX + 6 && mouseY >= thumbY && mouseY < thumbY + thumbH;
    }

    private void setScrollFromScrollbarY(double thumbY) {
        int maxScroll = maxScroll();
        int trackH = getHeight() - scrollbarThumbHeight();
        if (maxScroll <= 0 || trackH <= 0) {
            scrollY = 0;
            return;
        }
        double relativeThumbY = Math.clamp(thumbY - getY(), 0, trackH);
        scrollY = (relativeThumbY / trackH) * maxScroll;
    }

    private int indexAtClick(double clickY) {
        return ((int) clickY - getY() + (int) scrollY) / ROW_HEIGHT;
    }

    private boolean selectIndex(int idx) {
        if (idx < 0 || idx >= servers.size()) return false;
        selected = idx;
        notifySelectionChanged();
        return true;
    }

    private void notifySelectionChanged() {
        if (onSelectionChanged != null) onSelectionChanged.run();
    }
}
