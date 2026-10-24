package org.clearlag;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.api.ModInitializer;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

//create timers
public class ClearLag implements ModInitializer {
    private static final int INTERVAL_TICKS = (20 * 60 * 20) - (5 * 20); // 20 minutes (minus 5 seconds)
    private int ticksLeft = INTERVAL_TICKS;
    private static final int WARNING_TICKS = 5 * 20;
    private int clearCountdown = -1;

    @Override //restart timer in singleplayer
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> ticksLeft = INTERVAL_TICKS);
        //restart timer 2 as well
        ServerLifecycleEvents.SERVER_STARTED.register(server -> clearCountdown = -1);

        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

        //create /clearlag command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("clearlag")
                            .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                            .executes(context -> {
                                clearDroppedItems(context.getSource().getServer());
                                return 1;
                            })
            );
        });
    }

    private void onServerTick(MinecraftServer server) {
        //restart timer when it ends
        if (--ticksLeft <= 0) {
            ticksLeft = INTERVAL_TICKS;
            onTimerFinished(server);
        }

        if (clearCountdown > -1) {
            clearCountdown--;
            if (clearCountdown == 0) {
                clearDroppedItems(server);
                clearCountdown = -1;   // back to idle so it can't fire twice
            }
        }
    }

    private void onTimerFinished(MinecraftServer server) {
        //custom message for clearlag
        Component alertMessage = Component.empty()
                .append(Component.literal("[").withStyle(style -> style.withColor(0x55FFFF))) // aqua
                .append(Component.literal("SimpleClearLag").withStyle(style -> style.withColor(0x00AA00))) // dark_green
                .append(Component.literal("] ").withStyle(style -> style.withColor(0x55FFFF))) // aqua
                .append(Component.literal("Clearing ALL items in 5 Seconds!").withStyle(style -> style.withColor(0x00AA00))); // dark_green

        server.getPlayerList().broadcastSystemMessage(alertMessage, false);
        clearCountdown = WARNING_TICKS;   // starts the 5-second countdown
    }

    //run the kill commands
    public void clearDroppedItems(MinecraftServer server){
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
            //run a few commands to kill items
            server.getCommands().performPrefixedCommand(source, "kill @e[type=item]");
            server.getCommands().performPrefixedCommand(source, "kill @e[type=experience_orb]");
            server.getCommands().performPrefixedCommand(source, "kill @e[type=arrow,nbt={inGround:1b}]");
            server.getCommands().performPrefixedCommand(source, "kill @e[type=area_effect_cloud]");
            server.getCommands().performPrefixedCommand(source, "kill @e[type=trident,nbt={inGround:1b}]");

        Component alertMessage = Component.empty()
                .append(Component.literal("[").withStyle(style -> style.withColor(0x55FFFF))) // aqua
                .append(Component.literal("SimpleClearLag").withStyle(style -> style.withColor(0x00AA00))) // dark_green
                .append(Component.literal("] ").withStyle(style -> style.withColor(0x55FFFF))) // aqua
                .append(Component.literal("All items cleared!").withStyle(style -> style.withColor(0x00AA00))); // dark_green
                server.getPlayerList().broadcastSystemMessage(alertMessage, false);
    }
}