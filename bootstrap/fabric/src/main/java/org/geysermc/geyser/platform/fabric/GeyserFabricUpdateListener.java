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

package org.geysermc.geyser.platform.fabric;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.geysermc.geyser.Constants;
import org.geysermc.geyser.platform.fabric.command.FabricCommandSender;
import org.geysermc.geyser.util.VersionCheckUtils;

/**
 * Listens for player joins, and if they have the permission, notifies them if
 * there is a new Geyser update.
 */
public final class GeyserFabricUpdateListener {
    /**
     * Called when a player joins the server.
     * 
     * @param handler - the player's packet handler
     * @see {@link ServerGamePacketListenerImpl}
     */
    public static void onPlayReady(ServerGamePacketListenerImpl handler) {
        if (Permissions.check(handler.player, Constants.UPDATE_PERMISSION, 2)) {
            VersionCheckUtils.checkForGeyserUpdate(() -> new FabricCommandSender(handler.player.createCommandSourceStack()));
        }
    }

    private GeyserFabricUpdateListener() {
    }
}
