package com.mcsdc.addon.gui.vanilla;

public enum SearchFlag {
    YES(true),
    NO(false),
    ANY(null);

    public final Boolean bool;

    SearchFlag(Boolean bool) {
        this.bool = bool;
    }

    public SearchFlag next() {
        return switch (this) {
            case ANY -> YES;
            case YES -> NO;
            case NO -> ANY;
        };
    }

    public String label() {
        return switch (this) {
            case ANY -> "Any";
            case YES -> "Yes";
            case NO -> "No";
        };
    }
}
