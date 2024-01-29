package org.geysermc.geyser.session.cache;

import java.util.Map;
import java.util.Set;

import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.level.WorldManager;
import org.geysermc.geyser.session.GeyserSession;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;

@Getter
public class SessionCache {

    private final AdvancementsCache advancementsCache;
    private final BookEditCache bookEditCache;
    private final ChunkCache chunkCache;
    private final EntityCache entityCache;
    private final EntityEffectCache effectCache;
    private final FormCache formCache;
    private final LodestoneCache lodestoneCache;
    private final PistonCache pistonCache;
    private final PreferencesCache preferencesCache;
    private final SkullCache skullCache;
    private final TagCache tagCache;

    private final WeatherCache weatherCache;
    private final WorldCache worldCache;

    /**
     * A map of Vector3i positions to Java entities.
     * Used for translating Bedrock block actions to Java entity actions.
     */
    private final Map<Vector3i, ItemFrameEntity> itemFrameCache = new Object2ObjectOpenHashMap<>();

    /**
     * Stores a list of all lectern locations and their block entity tags.
     * See {@link WorldManager#sendLecternData(GeyserSession, int, int, int)}
     * for more information.
     */
    private final Set<Vector3i> lecternCache;

    public SessionCache(GeyserImpl geyser, GeyserSession session) {
        this.advancementsCache = new AdvancementsCache(session);
        this.bookEditCache = new BookEditCache(session);
        this.chunkCache = new ChunkCache(session);
        this.entityCache = new EntityCache(session);
        this.effectCache = new EntityEffectCache();
        this.formCache = new FormCache(session);
        this.lodestoneCache = new LodestoneCache();
        this.pistonCache = new PistonCache(session);
        this.preferencesCache = new PreferencesCache(session);
        this.skullCache = new SkullCache(session);
        this.tagCache = new TagCache();
        this.worldCache = new WorldCache(session);
        this.weatherCache = new WeatherCache();

        if (geyser.getWorldManager().shouldExpectLecternHandled(session)) {
            // Unneeded on these platforms
            this.lecternCache = null;
        } else {
            this.lecternCache = new ObjectOpenHashSet<>();
        }
    }

}
