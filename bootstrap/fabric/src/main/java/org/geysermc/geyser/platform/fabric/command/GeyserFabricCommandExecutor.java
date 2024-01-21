/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.platform.fabric.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandExecutor;
import org.geysermc.geyser.platform.fabric.GeyserFabricMod;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.GeyserLocale;

import java.util.Collections;

/**
 * This is the wrapper for the Fabric command executor.
 * It forwards all commands to the internal Geyser command executor.
 */
public class GeyserFabricCommandExecutor extends GeyserCommandExecutor implements Command<CommandSourceStack> {
    private final GeyserCommand command;

    /**
     * Creates a new Fabric command executor.
     * 
     * @param connector - the Geyser instance
     * @param command - the command to register
     */
    public GeyserFabricCommandExecutor(GeyserImpl connector, GeyserCommand command) {
        super(connector, Collections.singletonMap(command.name(), command));
        this.command = command;
    }

    public boolean testPermission(CommandSourceStack source) {
        return Permissions.check(source, command.permission(), command.isSuggestedOpOnly() ? 2 : 0);
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        return runWithArgs(context, "");
    }

    /**
     * Runs the command with the given arguments. This function will always 
     * return 0, unless an error occurs, in which case an exception will be thrown.
     * 
     * This function is used by the Brigadier command dispatcher, which uses status
     * codes to determine whether the command was successful. 
     * 
     * @see {@link Command}
     * 
     * @param context - the command context
     * @param args - the arguments
     * @return - 0 if the command was successful. 
     */
    public int runWithArgs(CommandContext<CommandSourceStack> context, String args) {
        CommandSourceStack source = context.getSource();
        FabricCommandSender sender = new FabricCommandSender(source);
        GeyserSession session = getGeyserSession(sender);
        if (!testPermission(source)) {
            sender.sendMessage(ChatColor.RED + GeyserLocale.getPlayerLocaleString("geyser.bootstrap.command.permission_fail", sender.locale()));
            return 0;
        }
        if (this.command.name().equals("reload")) {
            GeyserFabricMod.getInstance().setReloading(true);
        }

        if (command.isBedrockOnly() && session == null) {
            sender.sendMessage(ChatColor.RED + GeyserLocale.getPlayerLocaleString("geyser.bootstrap.command.bedrock_only", sender.locale()));
            return 0;
        }

        command.execute(session, sender, args.split(" "));
        return 0;
    }
}
