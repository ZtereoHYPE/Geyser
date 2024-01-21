package org.geysermc.geyser.api.block.custom.nonvanilla;

/**
 * Represents a bounding box that can be associated with a Java Edition block state, used for collision.
 */
public record JavaBoundingBox(double middleX, double middleY, double middleZ, double sizeX, double sizeY, double sizeZ) {
}
