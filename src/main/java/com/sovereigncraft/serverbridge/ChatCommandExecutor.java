package com.sovereigncraft.serverbridge;

import com.sovereigncraft.commands.Register;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import java.util.logging.Logger;

public class ChatCommandExecutor implements CommandExecutor {

    private final Logger logger;
    private final Register registerCmd;

    public ChatCommandExecutor(ServerBridge plugin, String homeserver, String adminToken) {
        this.logger = plugin.getLogger();
        this.registerCmd = new Register(homeserver, adminToken, plugin.getUserTracker(), plugin.getDataFolder(), this.logger);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("register")) {
            sender.sendMessage("§cUsage: /chat register");
            return true;
        }

        // Pass all arguments after "register" to Register command
        String[] subArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
        return registerCmd.onCommand(sender, command, label, subArgs);
    }
}
