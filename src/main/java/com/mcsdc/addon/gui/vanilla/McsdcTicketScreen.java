package com.mcsdc.addon.gui.vanilla;

import com.mcsdc.addon.gui.ServerInfoScreen;
import com.mcsdc.addon.util.TicketIDGenerator;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

public class McsdcTicketScreen extends McsdcParentScreen {
    private TextFieldWidget ticketField;
    private String status = "";

    public McsdcTicketScreen(Screen parent) {
        super(Text.literal("Ticket ID"), parent);
    }

    @Override
    protected void init() {
        ticketField = new TextFieldWidget(textRenderer, width / 2 - 100, height / 2 - 10, 200, 20, Text.literal("Ticket ID"));
        ticketField.setMaxLength(128);
        addDrawableChild(ticketField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Search"), b -> search())
            .dimensions(width / 2 - 50, height / 2 + 16, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
            .dimensions(width / 2 - 50, height / 2 + 44, 100, 20).build());
    }

    private void search() {
        String id = ticketField.getText().trim();
        if (id.isEmpty()) {
            status = "Enter a Ticket ID.";
            return;
        }
        try {
            String ip = TicketIDGenerator.decodeTicketID(id);
            if (!TicketIDGenerator.isValidIPv4WithPort(ip)) {
                status = "Invalid Ticket ID.";
                return;
            }
            client.setScreen(new ServerInfoScreen(ip));
        } catch (Exception e) {
            status = "Error decoding Ticket ID.";
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 36, Colors.WHITE);
        if (!status.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, status, width / 2, height / 2 + 70, Colors.LIGHT_RED);
        }
    }
}
