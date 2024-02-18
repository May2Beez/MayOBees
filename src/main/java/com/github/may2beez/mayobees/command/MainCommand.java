package com.github.may2beez.mayobees.command;

import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Main;
import com.github.may2beez.mayobees.MayOBees;

@Command(value = "m2b", description = "MayOBees Command", aliases = {"mayobees"})
public class MainCommand {

    @Main
    private void main() {
        MayOBees.CONFIG.openGui();
    }
}
