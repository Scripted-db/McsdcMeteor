package com.mcsdc.addon.gui.vanilla;

import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

import java.util.function.Consumer;

public class McsdcVersionSelectScreen extends McsdcParentScreen {
    private final SearchVersion selected;
    private final Consumer<SearchVersion> onSelect;

    public McsdcVersionSelectScreen(Screen parent, SearchVersion selected, Consumer<SearchVersion> onSelect) {
        super(Component.literal("Select Version"), parent);
        this.selected = selected;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        int listW = Math.min(240, width - 32);
        int listX = width / 2 - listW / 2;
        int top = 36;
        int bottom = height - 36;

        addRenderableWidget(new VersionListWidget(listX, top, listW, bottom - top, selected, version -> {
            onSelect.accept(version);
            onClose();
        }));

        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
            .bounds(width / 2 - 50, height - 28, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(font, title, width / 2, 12, CommonColors.WHITE);
    }

    private static final class VersionListWidget extends AbstractWidget {
        private static final int ROW_HEIGHT = 20;

        private final SearchVersion selected;
        private final Consumer<SearchVersion> onSelect;
        private double scrollY;
        private boolean draggingScrollbar;
        private double scrollbarDragOffset;

        private VersionListWidget(int x, int y, int width, int height, SearchVersion selected, Consumer<SearchVersion> onSelect) {
            super(x, y, width, height, Component.empty());
            this.selected = selected;
            this.onSelect = onSelect;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();

            context.fill(x, y, x + w, y + h, 0xC0101010);
            context.enableScissor(x, y, x + w, y + h);

            var font = Minecraft.getInstance().font;
            SearchVersion[] versions = SearchVersion.values();
            for (int i = 0; i < versions.length; i++) {
                int rowY = y + 1 + i * ROW_HEIGHT - (int) scrollY;
                if (rowY + ROW_HEIGHT < y) continue;
                if (rowY > y + h) break;

                SearchVersion version = versions[i];
                boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                boolean current = version == selected;

                if (current) context.fill(x, rowY, x + w, rowY + ROW_HEIGHT, 0x80606060);
                else if (hovered) context.fill(x, rowY, x + w, rowY + ROW_HEIGHT, 0x40404040);

                int color = current ? CommonColors.YELLOW : hovered ? CommonColors.WHITE : CommonColors.LIGHT_GRAY;
                context.text(font, version.version, x + 8, rowY + 6, color, true);
            }

            context.disableScissor();
            drawScrollbar(context);
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

            int index = ((int) event.y() - getY() + (int) scrollY) / ROW_HEIGHT;
            SearchVersion[] versions = SearchVersion.values();
            if (index >= 0 && index < versions.length) {
                onSelect.accept(versions[index]);
                return true;
            }
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

        private void drawScrollbar(GuiGraphicsExtractor context) {
            if (!hasScrollbar()) return;
            int barX = scrollbarX();
            int thumbY = scrollbarThumbY();
            int thumbH = scrollbarThumbHeight();
            context.fill(barX, getY(), barX + 5, getY() + getHeight(), 0x40FFFFFF);
            context.fill(barX, thumbY, barX + 5, thumbY + thumbH, 0xFFFFFFFF);
        }

        private boolean hasScrollbar() {
            return contentHeight() > getHeight();
        }

        private int contentHeight() {
            return SearchVersion.values().length * ROW_HEIGHT;
        }

        private int maxScroll() {
            return Math.max(0, contentHeight() - getHeight());
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
            int trackH = getHeight() - scrollbarThumbHeight();
            if (trackH <= 0) {
                scrollY = 0;
                return;
            }
            double relativeThumbY = Math.clamp(thumbY - getY(), 0, trackH);
            scrollY = (relativeThumbY / trackH) * maxScroll();
        }
    }
}
