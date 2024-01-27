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

package org.geysermc.geyser.platform.standalone.gui;

import lombok.Getter;

import java.awt.*;
import java.util.regex.Pattern;

/**
 * Represents a color that can be used in the console.
 * 
 * It will also map the color to a {@link Color} object.
 *
 * @see <a href="https://en.wikipedia.org/wiki/ANSI_escape_code#Colors">ANSI
 *      escape code colors</a>
 */
public enum ANSIColor {
    // Normal colors
    /** Black */
    BLACK("(0;)?30(0;)?m", Color.getHSBColor(0.000f, 0.000f, 0.000f)),

    /** Red */
    RED("(0;)?31(0;)?m", Color.getHSBColor(0.000f, 1.000f, 0.502f)),

    /** Green */
    GREEN("(0;)?32(0;)?m", Color.getHSBColor(0.333f, 1.000f, 0.502f)),

    /** Yellow */
    YELLOW("(0;)?33(0;)?m", Color.getHSBColor(0.167f, 1.000f, 0.502f)),

    /** Blue */
    BLUE("(0;)?34(0;)?m", Color.getHSBColor(0.667f, 1.000f, 0.502f)),

    /** Magenta */
    MAGENTA("(0;)?35(0;)?m", Color.getHSBColor(0.833f, 1.000f, 0.502f)),

    /** Cyan */
    CYAN("(0;)?36(0;)?m", Color.getHSBColor(0.500f, 1.000f, 0.502f)),

    /** White */
    WHITE("(0;)?37(0;)?m", Color.getHSBColor(0.000f, 0.000f, 0.753f)),

    // Bold colors
    /** Bold black */
    B_BLACK("(1;30|30;1)m", Color.getHSBColor(0.000f, 0.000f, 0.502f)),

    /** Bold red */
    B_RED("(1;31|31;1)m", Color.getHSBColor(0.000f, 1.000f, 1.000f)),

    /** Bold green */
    B_GREEN("(1;32|32;1)m", Color.getHSBColor(0.333f, 1.000f, 1.000f)),

    /** Bold yellow */
    B_YELLOW("(1;33|33;1)m", Color.getHSBColor(0.167f, 1.000f, 1.000f)),

    /** Bold blue */
    B_BLUE("(1;34|34;1)m", Color.getHSBColor(0.667f, 1.000f, 1.000f)),

    /** Bold magenta */
    B_MAGENTA("(1;35|35;1)m", Color.getHSBColor(0.833f, 1.000f, 1.000f)),

    /** Bold cyan */
    B_CYAN("(1;36|36;1)m", Color.getHSBColor(0.500f, 1.000f, 1.000f)),

    /** Bold white */
    B_WHITE("(1;37|37;1)m", Color.getHSBColor(0.000f, 0.000f, 1.000f)),

    /** Reset. This will clear all styles. */
    RESET("0m", Color.getHSBColor(0.000f, 0.000f, 1.000f));

    private static final ANSIColor[] VALUES = values();
    private static final String PREFIX = Pattern.quote("\u001B[");

    private final String ANSICode;

    @Getter
    private final Color color;

    ANSIColor(String ANSICode, Color color) {
        this.ANSICode = ANSICode;
        this.color = color;
    }

    /**
     * Gets the {@link ANSIColor} from the given ANSI code.
     *
     * @param code The ANSI code to get the color from.
     * @return The {@link ANSIColor} from the given ANSI code.
     */
    public static ANSIColor fromANSI(String code) {
        for (ANSIColor value : VALUES) {
            if (code.matches(PREFIX + value.ANSICode)) {
                return value;
            }
        }

        return B_WHITE;
    }
}
