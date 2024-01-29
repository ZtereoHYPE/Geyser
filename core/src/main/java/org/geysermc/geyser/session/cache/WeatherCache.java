package org.geysermc.geyser.session.cache;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class WeatherCache {
    /**
     * Caches current rain status.
     */
    private boolean raining = false;

    /**
     * Caches current thunder status.
     */
    private boolean thunder = false;
}
