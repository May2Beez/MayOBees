package com.github.may2beez.mayobees.event;

import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.eventhandler.Event;

public class ClickEntityEvent extends Event {
    public final Entity entity;

    protected ClickEntityEvent(Entity entity) {
        this.entity = entity;
    }

    public static class Left extends ClickEntityEvent {
        public Left(Entity entity) {
            super(entity);
        }
    }

    public static class Right extends ClickEntityEvent {
        public Right(Entity entity) {
            super(entity);
        }
    }
}
