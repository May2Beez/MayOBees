package com.github.may2beez.mayobees.feature;

import com.github.may2beez.mayobees.util.LogUtils;

public interface IFeature {
    boolean isRunning();
    String getName();
    void onEnable();
    void onDisable();

    default void log(String logMessage){
        LogUtils.debug(getMessage(logMessage));
    }

    default void info(String infoMessage){
        LogUtils.info(getMessage(infoMessage));
    }

    default void error(String errorMessage){
        LogUtils.error(getMessage(errorMessage));
    }

    default String getMessage(String message){
       return "[" + getName() + "] " + message;
    }
}
