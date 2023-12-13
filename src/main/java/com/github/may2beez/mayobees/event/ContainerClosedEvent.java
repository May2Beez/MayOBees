package com.github.may2beez.mayobees.event;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class ContainerClosedEvent extends Event {

    public Container container;

    public ContainerClosedEvent(Container container) {
        this.container = container;
    }

}