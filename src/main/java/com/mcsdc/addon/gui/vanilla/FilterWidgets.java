package com.mcsdc.addon.gui.vanilla;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Function;

public final class FilterWidgets {
    public static final int FILTER_WIDTH = 130;

    private FilterWidgets() {}

    private static <T> ButtonWidget cycle(
        String name,
        T initial,
        Function<T, T> next,
        Function<T, String> label,
        Consumer<T> onChange,
        int x,
        int y
    ) {
        @SuppressWarnings("unchecked")
        T[] current = (T[]) new Object[]{initial};
        return ButtonWidget.builder(Text.literal(name + ": " + label.apply(initial)), btn -> {
            current[0] = next.apply(current[0]);
            btn.setMessage(Text.literal(name + ": " + label.apply(current[0])));
            onChange.accept(current[0]);
        }).dimensions(x, y, FILTER_WIDTH, 20).build();
    }

    public static ButtonWidget cycleFlag(String name, SearchFlag initial, Consumer<SearchFlag> onChange, int x, int y) {
        return cycle(name, initial, SearchFlag::next, SearchFlag::label, onChange, x, y);
    }

    public static ButtonWidget toggle(String name, boolean initial, Consumer<Boolean> onChange, int x, int y) {
        return cycle(name, initial, b -> !b, b -> b ? "On" : "Off", onChange, x, y);
    }
}
