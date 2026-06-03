package com.mcsdc.addon.mixin;

import com.mcsdc.addon.gui.McsdcScreen;
import com.mcsdc.addon.gui.ServerInfoScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
    @Shadow
    protected MultiplayerServerListWidget serverListWidget;
    private ButtonWidget getInfoButton;

    protected MultiplayerScreenMixin() {
        super(null);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerScreen;updateButtonActivationStates()V"))
    private void onInit(CallbackInfo info) {
        this.addDrawableChild(
            new ButtonWidget.Builder(
                Text.literal("MCSDC"),
                onPress -> {
                    if (this.client == null) return;
                    McsdcScreen.open((Screen) (Object) this);
                }
            )
                .position(150, 3)
                .width(80)
                .build()
        );

        this.getInfoButton = this.addDrawableChild(
            new ButtonWidget.Builder(
                Text.literal("Server Info"),
                onPress -> {
                    if (this.client == null) return;
                    MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
                    if (entry != null) {
                        this.client.setScreen(new ServerInfoScreen(((MultiplayerServerListWidget.ServerEntry) entry).getServer().address));
                    }
                }
            )
                .position(150 + 80 + 5, 3)
                .width(80)
                .build()
        );
    }

    @Inject(method = "updateButtonActivationStates", at = @At("TAIL"))
    private void onUpdateButtonActivationStates(CallbackInfo info) {
        MultiplayerServerListWidget.Entry entry = this.serverListWidget.getSelectedOrNull();
        this.getInfoButton.active = entry != null && !(entry instanceof MultiplayerServerListWidget.ScanningEntry);
    }
}
