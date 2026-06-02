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

            CompletableFuture.supplyAsync(() -> parseLoginResponse(token)).thenAccept(result -> {
                mc.execute(() -> {
                    if (result.failed()) {
                        add(theme.label(result.error()));
                        return;
                    }

                    LoginResult response = result.login();
                    McsdcSystem.get().setToken(response.sessionToken());
                    McsdcSystem.get().setUsername(response.name());
                    McsdcSystem.get().setLevel(response.perms());

                    mc.setScreen(this.parent);
                    this.parent.reload();
                });
            });
        };
    }

    private static LoginParseResult parseLoginResponse(String token) {
        JsonObject body = new JsonObject();
        body.addProperty("token", token);

        String response = Api.postPublicJson("/auth/login", body);

        String err = Api.errorFrom(response);
        if (err != null) return LoginParseResult.failure(Api.loginFailureMessage(err));

        JsonObject data;
        try {
            data = Api.unwrapObject(response);
        } catch (Exception e) {
            return LoginParseResult.failure("invalid token");
        }

        err = Api.errorFrom(data);
        if (err != null) return LoginParseResult.failure(Api.loginFailureMessage(err));

        if (Api.isUserBanned(data)) return LoginParseResult.failure(Api.BAN_LOGIN_MSG);

        if (!data.has("token") || !data.has("name")) {
            return LoginParseResult.failure("invalid token");
        }

        return LoginParseResult.success(new LoginResult(
            data.get("token").getAsString(),
            data.get("name").getAsString(),
            data.has("perms") ? data.get("perms").getAsInt() : Api.roleToPerms(data.get("role"))
        ));
    }

    private record LoginResult(String sessionToken, String name, int perms) {}

    private record LoginParseResult(@Nullable LoginResult login, @Nullable String error) {
        static LoginParseResult success(LoginResult login) {
            return new LoginParseResult(login, null);
        }

        static LoginParseResult failure(String message) {
            return new LoginParseResult(null, message);
        }

        boolean failed() {
            return error != null;
        }
    }
}
