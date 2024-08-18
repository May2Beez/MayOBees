package com.github.may2beez.mayobees.event;

import lombok.Getter;
import net.minecraftforge.fml.common.eventhandler.Event;

@Getter
public class KeyTypedEvent extends Event {
    private final int keyCode;

    public KeyTypedEvent(int keyCode) {
        this.keyCode = keyCode;
    }
}
