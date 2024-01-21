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

package org.geysermc.geyser.platform.bungeecord;

import org.geysermc.geyser.GeyserMain;

/**
 * This is the BungeeCord implementation of {@link GeyserMain}.
 * 
 * It is used to show a message in case someone tries run the Geyser BungeeCord
 * plugin directly.
 */
public class GeyserBungeeMain extends GeyserMain {

    /**
     * This is the main method of the BungeeCord implementation of Geyser.
     * 
     * It just shows a message telling the user to put the plugin in the
     * plugins folder.
     * 
     * @param args - the command line arguments
     */
    public static void main(String[] args) {
        new GeyserBungeeMain().displayMessage();
    }

    @Override
    public String getPluginType() {
        return "BungeeCord";
    }

    @Override
    public String getPluginFolder() {
        return "plugins";
    }
}
