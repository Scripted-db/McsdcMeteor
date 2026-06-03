package com.mcsdc.addon.mixin;

import com.mcsdc.addon.gui.McsdcScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin() {
        super(null);
    }

    @ModifyVariable(method = "addNormalWidgets", at = @At(value = "CONSTANT", args = "stringValue=menu.online"), ordinal = 0)
    private int mcsdc$insertButton(int currentY, int y, int spacingY) {
        int mcsdcY = currentY + spacingY;
        this.addDrawableChild(
            ButtonWidget.builder(Text.literal("MCSDC"), btn -> {
                if (this.client == null) return;
                McsdcScreen.open((Screen) (Object) this);
            }).dimensions(this.width / 2 - 100, mcsdcY, 200, 20).build()
        );
        return mcsdcY;
    }
}
