package org.geysermc.geyser.api.block.custom.nonvanilla;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents a Java Edition block item.
 */
public record JavaBlockItem(@NonNull String identifier, @NonNegative int javaId, @NonNegative int stackSize) {
}
