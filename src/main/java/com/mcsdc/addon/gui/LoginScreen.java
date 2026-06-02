package com.mcsdc.addon.gui;

import com.google.gson.JsonObject;
import com.mcsdc.addon.Api;
import com.mcsdc.addon.system.McsdcSystem;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.StringSetting;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class LoginScreen extends WindowScreen {

    WindowScreen parent;

    private final Settings settings = new Settings();
    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<String> tokenSetting = sg.add(new StringSetting.Builder()
        .name("token")
        .description("The token to use for the API.")
        .defaultValue("")
        .build()
    );

    public LoginScreen(WindowScreen parent) {
        super(GuiThemes.get(), "Login with Token");
        this.parent = parent;
    }
    WContainer settingsContainer;

    @Override
    public void initWidgets() {
        WContainer settingsContainer = add(theme.verticalList()).expandX().widget();
        settingsContainer.add(theme.settings(settings)).expandX();

        this.settingsContainer = settingsContainer;

        add(theme.button("Submit")).expandX().widget().action = () -> {
            reload();

            String token = tokenSetting.get().trim();
            if (token.isEmpty()){
                add(theme.label("Please enter a token to login."));
                return;
            }

            CompletableFuture.supplyAsync(() -> parseLoginResponse(token)).thenAccept(response -> {
                mc.execute(() -> {
                    if (response == null) {
                        add(theme.label("Invalid token."));
                        return;
                    }

                    McsdcSystem.get().setToken(response.sessionToken());
                    McsdcSystem.get().setUsername(response.name());
                    McsdcSystem.get().setLevel(response.perms());

                    mc.setScreen(this.parent);
                    this.parent.reload();
                });
            });
        };
    }

    @Nullable
    private static LoginResult parseLoginResponse(String token) {
        JsonObject body = new JsonObject();
        body.addProperty("token", token);

        String response = Api.postPublicJson("/auth/login", body);
        if (response == null || response.isEmpty()) return null;

        JsonObject data;
        try {
            data = Api.unwrapObject(response);
        } catch (Exception e) {
            return null;
        }

        if (data.has("error") || !data.has("token") || !data.has("name")) return null;

        if (Api.isAccessBanned(data)) return null;

        return new LoginResult(
            data.get("token").getAsString(),
            data.get("name").getAsString(),
            data.has("perms") ? data.get("perms").getAsInt() : Api.roleToPerms(data.get("role"))
        );
    }

    private record LoginResult(String sessionToken, String name, int perms) {}
}
