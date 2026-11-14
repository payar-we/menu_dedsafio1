package com.hexasafio;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

/** Menu principal: video de fondo + cabecera + logo + botones JUGAR / OPCIONES / SALIR. */
public class HexaTitleScreen extends Screen {

    // ================== AJUSTES FACILES ==================
    /** Servidor al que entra el boton JUGAR. */
    static final String SERVER_NAME = "Nexsafio 4";
    static final String SERVER_IP = "nexsafio4.mcsh.gg";
    // =====================================================

    private static final int JUGAR_W = 243, JUGAR_H = 41;
    private static final int OPC_W = 379, OPC_H = 72;
    private static final int SALIR_W = 209, SALIR_H = 46;

    private static final float[] BTN_Y = {0.56f, 0.65f, 0.75f};

    private VideoBackground video;
    private long openMs = -1;
    private MenuUi.Btn[] buttons;

    public HexaTitleScreen() {
        super(Text.literal("Hexasafio"));
    }

    @Override
    protected void init() {
        if (video == null) {
            video = new VideoBackground();
            video.start(this.client);
        }
        if (openMs < 0) openMs = Util.getMeasuringTimeMs();

        buttons = new MenuUi.Btn[] {
            new MenuUi.Btn("jugar", JUGAR_W, JUGAR_H, this::joinServer),
            new MenuUi.Btn("opciones", OPC_W, OPC_H, () -> this.client.setScreen(new OptionsScreen(this, this.client.options))),
            new MenuUi.Btn("salir", SALIR_W, SALIR_H, () -> this.client.scheduleStop())
        };
    }

    private void joinServer() {
        ServerInfo info = new ServerInfo(SERVER_NAME, SERVER_IP, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(this, this.client, ServerAddress.parse(SERVER_IP), info, false, null);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Util.getMeasuringTimeMs();
        float fade = MathHelper.clamp((now - openMs) / 900f, 0f, 1f);
        RenderSystem.enableBlend();

        if (video != null) {
            video.draw(ctx, this.client, this.width, this.height, now);
        }
        MenuUi.draw(this.client, ctx, this.width, this.height, mouseX, mouseY, now, fade,
                0.31f, 0.024f, 0.055f, 0.28f, BTN_Y, OPC_W, 0.105f, buttons);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            MenuUi.Btn b = MenuUi.hit(buttons, mouseX, mouseY);
            if (b != null) {
                this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                b.action.run();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // sin fondo/blur vanilla: el video lo dibuja render()
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void removed() {
        if (video != null) {
            video.close();
            video = null;
        }
    }
}
