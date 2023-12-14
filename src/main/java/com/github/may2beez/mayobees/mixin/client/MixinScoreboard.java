package com.github.may2beez.mayobees.mixin.client;

import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = Scoreboard.class, priority = Integer.MAX_VALUE)
public abstract class MixinScoreboard {
    @Inject(method = "removeTeam", at = @At("HEAD"), cancellable = true)
    private void patcher$checkIfTeamIsNull(ScorePlayerTeam team, CallbackInfo ci) {
        if (team == null) {
            ci.cancel();
        }
    }

    @Redirect(method = "removeTeam", at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 0, remap = false))
    private <K, V> V patcher$checkIfRegisteredNameIsNull(Map<K, V> instance, K o) {
        if (o != null) return instance.remove(o);
        return null;
    }

    @Inject(method = "removeObjective", at = @At("HEAD"), cancellable = true)
    private void patcher$checkIfObjectiveIsNull(ScoreObjective objective, CallbackInfo ci) {
        if (objective == null) {
            ci.cancel();
        }
    }

    @Redirect(method = "removeObjective", at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 0, remap = false))
    private <K, V> V patcher$checkIfNameIsNull(Map<K, V> instance, K o) {
        if (o != null) return instance.remove(o);
        return null;
    }
}
