package com.mcsdc.addon.mixin;

import com.mcsdc.addon.gui.McsdcScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin() {
        super(null);
    }

    @ModifyVariable(method = "createNormalMenuOptions", at = @At(value = "CONSTANT", args = "stringValue=menu.online"), ordinal = 0)
    private int mcsdc$insertButton(int currentY, int topPos, int spacing) {
        int mcsdcY = currentY + spacing;
        this.addRenderableWidget(
            Button.builder(Component.literal("MCSDC"), btn -> {
                if (this.minecraft == null) return;
                McsdcScreen.open((Screen) (Object) this);
            }).bounds(this.width / 2 - 100, mcsdcY, 200, 20).build()
        );
        return mcsdcY;
    }
}
