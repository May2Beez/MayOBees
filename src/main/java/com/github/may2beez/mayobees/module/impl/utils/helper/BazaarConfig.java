package com.github.may2beez.mayobees.module.impl.utils.helper;

import lombok.Setter;
import lombok.experimental.Accessors;

// Just to make it reusable
public class BazaarConfig {
    @Setter
    @Accessors(chain = true)
    public String itemToBuy;

    @Setter
    @Accessors(chain = true)
    public int buyAmount;

    @Setter
    @Accessors(chain = true)
    public int spendThreshold = 0;
    public int guiClickDelay;
    public int guiClickDelayRandomness;
    public int guiWaitTimeout;

    public BazaarConfig(String itemToBuy, int buyAmount, int spendThreshold, int guiClickDelay, int guiClickDelayRandomness, int guiWaitTimeout) {
        this.itemToBuy = itemToBuy;
        this.buyAmount = buyAmount;
        this.spendThreshold = spendThreshold;
        this.guiClickDelay = guiClickDelay;
        this.guiClickDelayRandomness = guiClickDelayRandomness;
        this.guiWaitTimeout = guiWaitTimeout;
    }

    public BazaarConfig(int guiClickDelay, int guiClickDelayRandomness, int guiWaitTimeout) {
        this.guiClickDelay = guiClickDelay;
        this.guiClickDelayRandomness = guiClickDelayRandomness;
        this.guiWaitTimeout = guiWaitTimeout;
    }

    public boolean verifyConfig() {
        return itemToBuy == null || buyAmount <= 0 || spendThreshold < 0 || guiClickDelay <= 0 || guiClickDelayRandomness < 0 || guiWaitTimeout < 0;
    }

    @Override
    public String toString() {
        return "BazaarConfig{" +
                "itemToBuy='" + itemToBuy + '\'' +
                ", buyAmount=" + buyAmount +
                ", spendThreshold=" + spendThreshold +
                ", guiClickDelay=" + guiClickDelay +
                ", guiClickDelayRandomness=" + guiClickDelayRandomness +
                ", guiWaitTimeout=" + guiWaitTimeout +
                '}';
    }
}
