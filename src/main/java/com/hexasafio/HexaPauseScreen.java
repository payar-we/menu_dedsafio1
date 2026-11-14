package com.hexasafio;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

/** Menu de pausa (tecla ESC dentro del juego): VOLVER AL JUEGO / OPCIONES / DESCONECTARSE. */
public class HexaPauseScreen extends Screen {

    private static final int VOLVER_W = 839, VOLVER_H = 52;
    private static final int OPC_W = 460, OPC_H = 55;
    private static final int DESC_W = 776, DESC_H = 54;

    private static final float[] BTN_Y = {0.56f, 0.68f, 0.80f};

    private long openMs = -1;
    private MenuUi.Btn[] buttons;

    public HexaPauseScreen() {
        super(Text.literal("Pausa"));
    }

    @Override
    protected void init() {
        if (openMs < 0) openMs = Util.getMeasuringTimeMs();
        buttons = new MenuUi.Btn[] {
            new MenuUi.Btn("p_volver", VOLVER_W, VOLVER_H, this::close),
            new MenuUi.Btn("p_opciones", OPC_W, OPC_H, () -> this.client.setScreen(new OptionsScreen(this, this.client.options))),
            new MenuUi.Btn("p_desconectar", DESC_W, DESC_H, this::disconnect)
        };
    }

    private void disconnect() {
        boolean single = this.client.isInSingleplayer();
        if (this.client.world != null) {
            this.client.world.disconnect();
        }
        if (single) {
            this.client.disconnect(new MessageScreen(Text.translatable("menu.savingLevel")));
        } else {
            this.client.disconnect();
        }
        this.client.setScreen(new TitleScreen());   // el mixin lo cambia por el menu de Hexasafio
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Util.getMeasuringTimeMs();
        float fade = MathHelper.clamp((now - openMs) / 350f, 0f, 1f);
        RenderSystem.enableBlend();

        this.renderBackground(ctx, mouseX, mouseY, delta);   // mundo oscurecido/borroso, como la pausa normal
        MenuUi.draw(this.client, ctx, this.width, this.height, mouseX, mouseY, now, fade,
                0.5f, 0.06f, 0.11f, 0.26f, BTN_Y, OPC_W, 0.12f, buttons);
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
    public boolean shouldCloseOnEsc() {
        return true;   // ESC = volver al juego
    }
}
