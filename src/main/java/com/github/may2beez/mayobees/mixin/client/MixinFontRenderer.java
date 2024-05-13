package com.github.may2beez.mayobees.mixin.client;

import com.github.may2beez.mayobees.module.impl.player.NickHider;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FontRenderer.class)
public class MixinFontRenderer {
    @ModifyVariable(method = "renderString", at = @At("HEAD"), argsOnly = true)
    private String nickedName(String name) {
        if (!NickHider.getInstance().isRunning()) return name;
        return NickHider.getInstance().apply(name);
    }

    @ModifyVariable(method = "getStringWidth", at = @At("HEAD"), argsOnly = true)
    private String nickedWidth(String name) {
        if (!NickHider.getInstance().isRunning()) return name;
        return NickHider.getInstance().apply(name);
    }
}
