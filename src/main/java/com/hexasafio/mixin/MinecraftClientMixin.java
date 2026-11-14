package com.hexasafio.mixin;

import com.hexasafio.HexaPauseScreen;
import com.hexasafio.HexaTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cambia el menu principal (TitleScreen) por HexaTitleScreen y el menu de pausa (GameMenuScreen)
 * por HexaPauseScreen. Funciona tambien cuando otros mods usan su propia version/subclase de esos menus.
 */
@Mixin(value = MinecraftClient.class, priority = 1500)
public abstract class MinecraftClientMixin {

    @Shadow public Screen currentScreen;
    @Shadow public abstract void setScreen(Screen screen);

    /** 1) Al abrir cualquier pantalla. */
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen hexasafio$replaceScreen(Screen screen) {
        if (screen instanceof TitleScreen) {
            return new HexaTitleScreen();
        }
        if (screen instanceof GameMenuScreen) {
            return new HexaPauseScreen();
        }
        return screen;
    }

    /** 2) Red de seguridad: si otro mod puso su menu por otro camino, lo cambiamos en el siguiente tick. */
    @Inject(method = "tick", at = @At("HEAD"))
    private void hexasafio$tick(CallbackInfo ci) {
        Screen s = this.currentScreen;
        if (s instanceof TitleScreen) {
            this.setScreen(new HexaTitleScreen());
        } else if (s instanceof GameMenuScreen) {
            this.setScreen(new HexaPauseScreen());
        }
    }
}
