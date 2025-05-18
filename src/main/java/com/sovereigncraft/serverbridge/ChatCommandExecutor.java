package com.sovereigncraft.serverbridge;

import com.sovereigncraft.commands.Register;
import com.sovereigncraft.commands.UnRegister;
import com.sovereigncraft.commands.SetAvatar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ChatCommandExecutor implements CommandExecutor {

    private final Register registerCmd;
    private final UnRegister unregisterCmd;
    private final SetAvatar setAvatarCmd;

    public ChatCommandExecutor(ServerBridge plugin, String homeserver, String adminToken) {
        // Initialize your subcommands here, passing required dependencies
        this.registerCmd = new Register(homeserver, adminToken, plugin.getUserTracker(), plugin.getDataFolder());
        this.unregisterCmd = new UnRegister(homeserver, plugin.getUserTracker(), plugin.getDataFolder());
        this.setAvatarCmd = new SetAvatar(homeserver, adminToken);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /chat <register|unregister|setavatar>");
            return true;
        }

        String subcommand = args[0].toLowerCase();
        String[] subArgs = java.util.Arrays.copyOfRange(args, 1, args.length);

        switch (subcommand) {
            case "register":
                return registerCmd.onCommand(sender, command, label, subArgs);
            case "unregister":
                return unregisterCmd.onCommand(sender, command, label, subArgs);
            case "setavatar":
                return setAvatarCmd.onCommand(sender, command, label, subArgs);
            default:
                sender.sendMessage("§cUnknown subcommand. Usage: /chat <register|unregister|setavatar>");
                return true;
        }
    }
}
