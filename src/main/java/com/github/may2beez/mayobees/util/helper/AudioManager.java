package com.github.may2beez.mayobees.util.helper;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.sound.sampled.Clip;

public class AudioManager {
    private final Minecraft mc = Minecraft.getMinecraft();
    private static AudioManager instance;

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    @Getter
    @Setter
    private boolean minecraftSoundEnabled = false;

    private final Clock delayBetweenPings = new Clock();
    private int numSounds = 15;
    @Setter
    private float soundBeforeChange = 0;

    public void resetSound() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
            return;
        }
        minecraftSoundEnabled = false;
    }

    private static Clip clip;

    public void playSound() {
        if (minecraftSoundEnabled) return;
        numSounds = 15;
        minecraftSoundEnabled = true;
    }

    public boolean isSoundPlaying() {
        return (clip != null && clip.isRunning()) || minecraftSoundEnabled;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!minecraftSoundEnabled) return;
        if (delayBetweenPings.isScheduled() && !delayBetweenPings.passed()) return;
        if (numSounds <= 0) {
            minecraftSoundEnabled = false;
            return;
        }

        switch (MayOBeesConfig.failsafeSoundSelected) {
            case 0: {
                mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, "random.orb", 10.0F, 1.0F, false);
                break;
            }
            case 1: {
                mc.theWorld.playSound(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, "random.anvil_land", 10.0F, 1.0F, false);
                break;
            }
        }
        delayBetweenPings.schedule(100);
        numSounds--;
    }
}
