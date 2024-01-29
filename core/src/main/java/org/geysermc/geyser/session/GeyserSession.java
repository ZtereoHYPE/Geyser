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

package org.geysermc.geyser.session;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.IntRange;
import org.cloudburstmc.math.vector.Vector2i;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import org.cloudburstmc.protocol.bedrock.data.GameRuleData;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumData;
import org.cloudburstmc.protocol.bedrock.data.command.SoftEnumUpdateType;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.ChunkRadiusUpdatedPacket;
import org.cloudburstmc.protocol.bedrock.packet.ClientboundMapItemDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.GameRulesChangedPacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEvent2Packet;
import org.cloudburstmc.protocol.bedrock.packet.TextPacket;
import org.cloudburstmc.protocol.bedrock.packet.TransferPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateSoftEnumPacket;
import org.geysermc.api.util.BedrockPlatform;
import org.geysermc.api.util.InputMode;
import org.geysermc.api.util.UiProfile;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.bedrock.camera.CameraShake;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.entity.type.GeyserEntity;
import org.geysermc.geyser.api.entity.type.player.GeyserPlayerEntity;
import org.geysermc.geyser.api.network.RemoteServer;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.configuration.GeyserConfiguration;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.entity.type.Tickable;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.erosion.AbstractGeyserboundPacketHandler;
import org.geysermc.geyser.erosion.GeyserboundHandshakePacketHandler;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.inventory.recipe.GeyserRecipe;
import org.geysermc.geyser.inventory.recipe.GeyserStonecutterData;
import org.geysermc.geyser.level.JavaDimension;
import org.geysermc.geyser.level.WorldManager;
import org.geysermc.geyser.level.physics.CollisionManager;
import org.geysermc.geyser.registry.type.BlockMappings;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.auth.AuthData;
import org.geysermc.geyser.session.auth.BedrockClientData;
import org.geysermc.geyser.session.cache.AdvancementsCache;
import org.geysermc.geyser.session.cache.BookEditCache;
import org.geysermc.geyser.session.cache.ChunkCache;
import org.geysermc.geyser.session.cache.EntityCache;
import org.geysermc.geyser.session.cache.EntityEffectCache;
import org.geysermc.geyser.session.cache.FormCache;
import org.geysermc.geyser.session.cache.LodestoneCache;
import org.geysermc.geyser.session.cache.PistonCache;
import org.geysermc.geyser.session.cache.PreferencesCache;
import org.geysermc.geyser.session.cache.SkullCache;
import org.geysermc.geyser.session.cache.TagCache;
import org.geysermc.geyser.session.cache.TeleportCache;
import org.geysermc.geyser.session.cache.WorldBorder;
import org.geysermc.geyser.session.cache.WorldCache;
import org.geysermc.geyser.text.TextDecoration;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Pose;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.entity.player.HandPreference;
import com.github.steveice10.mc.protocol.data.game.setting.ChatVisibility;
import com.github.steveice10.mc.protocol.data.game.setting.SkinPart;
import com.github.steveice10.mc.protocol.data.game.statistic.CustomStatistic;
import com.github.steveice10.mc.protocol.data.game.statistic.Statistic;
import com.github.steveice10.mc.protocol.packet.common.serverbound.ServerboundClientInformationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import com.github.steveice10.mc.protocol.packet.login.serverbound.ServerboundCustomQueryAnswerPacket;
import com.github.steveice10.packetlib.packet.Packet;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
public class GeyserSession implements GeyserConnection, GeyserCommandSource {

    private final GeyserImpl geyser;
    private final UpstreamSession upstream;

    @Setter(AccessLevel.PACKAGE)
    private DownstreamSession downstream;
    /**
     * The loop where all packets and ticking is processed to prevent concurrency
     * issues.
     * If this is manually called, ensure that any exceptions are properly handled.
     */
    private final EventLoop eventLoop;
    @Setter
    private AuthData authData;
    @Setter
    private BedrockClientData clientData;
    /**
     * Used for Floodgate skin uploading
     */
    @Setter
    private List<String> certChainData;

    /**
     * This stores all player related information about the session,
     * as well as certain methods.
     */
    private final PlayerSessionInformation sessionInformation;

    /**
     * This handles a lot of the connection management
     */
    private SessionConnectionManager connectionManager;

    @NonNull
    @Setter
    private AbstractGeyserboundPacketHandler erosionHandler;

    @Accessors(fluent = true)
    @Setter
    private RemoteServer remoteServer;

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
    private final WorldCache worldCache;

    @Setter
    private TeleportCache unconfirmedTeleport;

    private final WorldBorder worldBorder;

    @Setter
    private InventoryTranslator inventoryTranslator = InventoryTranslator.PLAYER_INVENTORY_TRANSLATOR;

    /**
     * Use {@link #getNextItemNetId()} instead for consistency
     */
    @Getter(AccessLevel.NONE)
    private final AtomicInteger itemNetId = new AtomicInteger(2);

    @Setter
    private ScheduledFuture<?> craftingGridFuture;

    /**
     * Stores session collision
     */
    private final CollisionManager collisionManager;

    /**
     * Stores the block mappings for this specific version.
     */
    @Setter
    private BlockMappings blockMappings;

    /**
     * Stores the item translations for this specific version.
     */
    @Setter
    private ItemMappings itemMappings;

    private final Long2ObjectMap<ClientboundMapItemDataPacket> storedMaps = new Long2ObjectOpenHashMap<>();

    /**
     * Required to decode biomes correctly.
     */
    @Setter
    private int biomeGlobalPalette;
    /**
     * Stores the map between Java and Bedrock biome network IDs.
     */
    @Setter
    private int[] biomeTranslations = null;

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

    /**
     * A list of all players that have a player head on with a custom texture.
     * Our workaround for these players is to give them a custom skin and geometry to emulate wearing a custom skull.
     */
    private final Set<UUID> playerWithCustomHeads = new ObjectOpenHashSet<>();

    @Setter
    private boolean droppingLecternBook;

    @Setter
    private Vector2i lastChunkPosition = null;
    @Setter
    private int clientRenderDistance = -1;
    private int serverRenderDistance = -1;

    /**
     * Accessed on the initial Java and Bedrock packet processing threads
     */
    @Setter(AccessLevel.PACKAGE)
    private volatile boolean closed;

    /**
     * All dimensions that the client could possibly connect to.
     */
    private final Map<String, JavaDimension> dimensions = new Object2ObjectOpenHashMap<>(3);

    private final Int2ObjectMap<TextDecoration> chatTypes = new Int2ObjectOpenHashMap<>(7);

    /**
     * Stores all Java recipes by recipe identifier, and matches them to all possible Bedrock recipe identifiers.
     * They are not 1:1, since Bedrock can have multiple recipes for the same Java recipe.
     */
    @Setter
    private Map<String, List<String>> javaToBedrockRecipeIds;

    @Setter
    private Int2ObjectMap<GeyserRecipe> craftingRecipes;
    private final AtomicInteger lastRecipeNetId;

    /**
     * Saves a list of all stonecutter recipes, for use in a stonecutter inventory.
     * The key is the Java ID of the item; the values are all the possible outputs'
     * Java IDs sorted by their string identifier
     */
    @Setter
    private Int2ObjectMap<GeyserStonecutterData> stonecutterRecipes;

    /**
     * Whether to work around 1.13's different behavior in villager trading menus.
     */
    @Setter
    private boolean emulatePost1_13Logic = true;
    /**
     * Starting in 1.17, Java servers expect the <code>carriedItem</code> parameter of the serverbound click container
     * packet to be the current contents of the mouse after the transaction has been done. 1.16 expects the clicked slot
     * contents before any transaction is done. With the current ViaVersion structure, if we do not send what 1.16 expects
     * and send multiple click container packets, then successive transactions will be rejected.
     */
    @Setter
    private boolean emulatePost1_16Logic = true;
    @Setter
    private boolean emulatePost1_18Logic = true;

    /**
     * Whether to emulate pre-1.20 smithing table behavior.
     * Adapts ViaVersion's furnace UI to one Bedrock can use.
     * See {@link org.geysermc.geyser.translator.inventory.OldSmithingTableTranslator}.
     */
    @Setter
    private boolean oldSmithingTable = false;

    /**
     * Caches current rain status.
     */
    @Setter
    private boolean raining = false;

    /**
     * Caches current thunder status.
     */
    @Setter
    private boolean thunder = false;

    /**
     * Stores a map of all statistics sent from the server.
     * The server only sends new statistics back to us, so in order to show all statistics we need to cache existing ones.
     */
    private final Object2IntMap<Statistic> statistics = new Object2IntOpenHashMap<>(0);

    /**
     * Whether we're expecting statistics to be sent back to us.
     */
    @Setter
    private boolean waitingForStatistics = false;

    /**
     * The thread that will run every 50 milliseconds - one Minecraft tick.
     */
    private ScheduledFuture<?> tickThread = null;

    /**
     * Used to return the player to their original rotation after using an item in BedrockInventoryTransactionTranslator
     */
    @Setter
    private ScheduledFuture<?> lookBackScheduledFuture = null;

    /**
     * Used to return players back to their vehicles if the server doesn't want them unmounting.
     */
    @Setter
    private ScheduledFuture<?> mountVehicleScheduledFuture = null;

    /**
     * A cache of IDs from ClientboundKeepAlivePackets that have been sent to the Bedrock client, but haven't been returned to the server.
     * Only used if {@link GeyserConfiguration#isForwardPlayerPing()} is enabled.
     */
    private final Queue<Long> keepAliveCache = new ConcurrentLinkedQueue<>();

    @Setter(AccessLevel.PACKAGE)
    private MinecraftProtocol protocol;

    public GeyserSession(GeyserImpl geyser, BedrockServerSession bedrockServerSession, EventLoop eventLoop) {
        this.geyser = geyser;
        this.upstream = new UpstreamSession(bedrockServerSession);
        this.eventLoop = eventLoop;

        this.erosionHandler = new GeyserboundHandshakePacketHandler(this);

        this.advancementsCache = new AdvancementsCache(this);
        this.bookEditCache = new BookEditCache(this);
        this.chunkCache = new ChunkCache(this);
        this.entityCache = new EntityCache(this);
        this.effectCache = new EntityEffectCache();
        this.formCache = new FormCache(this);
        this.lodestoneCache = new LodestoneCache();
        this.pistonCache = new PistonCache(this);
        this.preferencesCache = new PreferencesCache(this);
        this.skullCache = new SkullCache(this);
        this.tagCache = new TagCache();
        this.worldCache = new WorldCache(this);

        this.worldBorder = new WorldBorder(this);

        this.collisionManager = new CollisionManager(this);

        this.sessionInformation = new PlayerSessionInformation(this, geyser);
        this.connectionManager = new SessionConnectionManager(this, geyser);

        collisionManager.updatePlayerBoundingBox(this.getPlayerEntity().getPosition());

        this.craftingRecipes = new Int2ObjectOpenHashMap<>();
        this.javaToBedrockRecipeIds = new Object2ObjectOpenHashMap<>();
        this.lastRecipeNetId = new AtomicInteger(1);

        if (geyser.getWorldManager().shouldExpectLecternHandled(this)) {
            // Unneeded on these platforms
            this.lecternCache = null;
        } else {
            this.lecternCache = new ObjectOpenHashSet<>();
        }

        this.remoteServer = geyser.defaultRemoteServer();
    }

    /**
     * Moves task to the session event loop if already not in it. Otherwise, the
     * task is automatically ran.
     */
    public void ensureInEventLoop(Runnable runnable) {
        if (eventLoop.inEventLoop()) {
            runnable.run();
            return;
        }
        executeInEventLoop(runnable);
    }

    /**
     * Executes a task and prints a stack trace if an error occurs.
     */
    public void executeInEventLoop(Runnable runnable) {
        eventLoop.execute(() -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                geyser.getLogger().error("Error thrown in " + this.bedrockUsername() + "'s event loop!", e);
            }
        });
    }

    public boolean isLoggingIn() {
        return this.connectionManager.isLoggingIn();
    }

    public boolean isLoggedIn() {
        return this.connectionManager.isLoggedIn();
    }

    /**
     * Schedules a task and prints a stack trace if an error occurs.
     */
    public ScheduledFuture<?> scheduleInEventLoop(Runnable runnable, long duration, TimeUnit timeUnit) {
        return eventLoop.schedule(() -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                geyser.getLogger().error("Error thrown in " + this.bedrockUsername() + "'s event loop!", e);
            }
        }, duration, timeUnit);
    }

    protected void setupTickThread() {
        tickThread = eventLoop.scheduleAtFixedRate(this::tick, 50, 50, TimeUnit.MILLISECONDS);
    }

    protected void cancelTickThread() {
        if (tickThread != null) {
            tickThread.cancel(false);
        }
    }

    /**
     * Called every 50 milliseconds - one Minecraft tick.
     */
    protected void tick() {
        try {
            pistonCache.tick();
            // Check to see if the player's position needs updating - a position update
            // should be sent once every 3 seconds
            if (this.isSpawned() &&
                    (System.currentTimeMillis() - this.getLastMovementTimestamp()) > 3000) {
                PlayerEntity playerEntity = this.getPlayerEntity();

                // Recalculate in case something else changed position
                Vector3d position = collisionManager.adjustBedrockPosition(playerEntity.getPosition(),
                        playerEntity.isOnGround(), false);
                // A null return value cancels the packet
                if (position != null) {
                    ServerboundMovePlayerPosPacket packet = new ServerboundMovePlayerPosPacket(
                            playerEntity.isOnGround(),
                            position.getX(), position.getY(), position.getZ());
                    sendDownstreamGamePacket(packet);
                }
                this.setLastMovementTimestamp(System.currentTimeMillis());
            }

            if (worldBorder.isResizing()) {
                worldBorder.resize();
            }

            boolean shouldShowFog = !worldBorder.isWithinWarningBoundaries();
            boolean isInWorldBorderWarningArea = this.isInWorldBorderWarningArea();
            if (shouldShowFog || worldBorder.isCloseToBorderBoundaries()) {
                // Show particles representing where the world border is
                worldBorder.drawWall();
                // Set the mood
                if (shouldShowFog && !isInWorldBorderWarningArea) {
                    this.setInWorldBorderWarningArea(true);
                    sendFog("minecraft:fog_crimson_forest");
                }
            }
            if (!shouldShowFog && isInWorldBorderWarningArea) {
                // Clear fog as we are outside the world border now
                removeFog("minecraft:fog_crimson_forest");
                this.setInWorldBorderWarningArea(false);
            }

            for (Tickable entity : entityCache.getTickableEntities()) {
                entity.tick();
            }

            if (getArmAnimationTicks() >= 0) {
                // As of 1.18.2 Java Edition, it appears that the swing time is dynamically
                // updated depending on the
                // player's effect status, but the animation can cut short if the duration
                // suddenly decreases
                // (from suddenly no longer having mining fatigue, for example)
                // This math is referenced from Java Edition 1.18.2
                int swingTotalDuration;
                int hasteLevel = Math.max(effectCache.getHaste(), effectCache.getConduitPower());
                if (hasteLevel > 0) {
                    swingTotalDuration = 6 - hasteLevel;
                } else {
                    int miningFatigueLevel = effectCache.getMiningFatigue();
                    if (miningFatigueLevel > 0) {
                        swingTotalDuration = 6 + miningFatigueLevel * 2;
                    } else {
                        swingTotalDuration = 6;
                    }
                }

                setArmAnimationTicks(getArmAnimationTicks() + 1);
                if (getArmAnimationTicks() >= swingTotalDuration) {
                    if (isSneaking()) {
                        // Attempt to re-activate blocking as our swing animation is up
                        if (this.sessionInformation.attemptToBlock()) {
                            getPlayerEntity().updateBedrockMetadata();
                        }
                    }
                    setArmAnimationTicks(-1);
                }
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void setAuthenticationData(AuthData authData) {
        this.authData = authData;
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public void sendMessage(@NonNull String message) {
        TextPacket textPacket = new TextPacket();
        textPacket.setPlatformChatId("");
        textPacket.setSourceName("");
        textPacket.setXuid("");
        textPacket.setType(TextPacket.Type.CHAT);
        textPacket.setNeedsTranslation(false);
        textPacket.setMessage(message);

        upstream.sendPacket(textPacket);
    }

    @Override
    public boolean isConsole() {
        return false;
    }

    @Override
    public String locale() {
        return clientData.getLanguageCode();
    }

    /**
     * Sends a chat message to the Java server.
     */
    public void sendChat(String message) {
        sendDownstreamGamePacket(
                new ServerboundChatPacket(message, Instant.now().toEpochMilli(), 0L, null, 0, new BitSet()));
    }

    /**
     * Sends a command to the Java server.
     */
    public void sendCommand(String command) {
        sendDownstreamGamePacket(new ServerboundChatCommandPacket(command, Instant.now().toEpochMilli(), 0L,
                Collections.emptyList(), 0, new BitSet()));
    }

    public void setServerRenderDistance(int renderDistance) {
        this.serverRenderDistance = renderDistance;

        ChunkRadiusUpdatedPacket chunkRadiusUpdatedPacket = new ChunkRadiusUpdatedPacket();
        chunkRadiusUpdatedPacket.setRadius(renderDistance);
        upstream.sendPacket(chunkRadiusUpdatedPacket);
    }

    public InetSocketAddress getSocketAddress() {
        return this.upstream.getAddress();
    }

    public void connect() {
        this.connectionManager.connect();
    }

    public void authenticate(String username) {
        this.connectionManager.authenticate(username);
    }

    public void authenticateWithRefreshToken(String refreshToken) {
        this.connectionManager.authenticateWithRefreshToken(refreshToken);
    }

    public void authenticateWithMicrosoftCode() {
        this.connectionManager.authenticateWithMicrosoftCode();
    }

    public void authenticateWithMicrosoftCode(boolean offlineAccess) {
        this.connectionManager.authenticateWithMicrosoftCode(offlineAccess);
    }

    public boolean onMicrosoftLoginComplete(PendingMicrosoftAuthentication.AuthenticationTask task) {
        return this.connectionManager.onMicrosoftLoginComplete(task);
    }

    public void disconnect(String reason) {
        this.connectionManager.disconnect(reason);
    }

    @Override
    public boolean sendForm(@NonNull Form form) {
        formCache.showForm(form);
        return true;
    }

    @Override
    public boolean sendForm(@NonNull FormBuilder<?, ?, ?> formBuilder) {
        formCache.showForm(formBuilder.build());
        return true;
    }

    /**
     * @deprecated since Cumulus version 1.1, and will be removed when Cumulus 2.0
     *             releases. Please use the new forms instead.
     */
    @Deprecated
    public void sendForm(org.geysermc.cumulus.Form<?> form) {
        sendForm(form.newForm());
    }

    /**
     * @deprecated since Cumulus version 1.1, and will be removed when Cumulus 2.0
     *             releases. Please use the new forms instead.
     */
    @Deprecated
    public void sendForm(org.geysermc.cumulus.util.FormBuilder<?, ?> formBuilder) {
        sendForm(formBuilder.build());
    }

    /**
     * @return the next Bedrock item network ID to use for a new item
     */
    public int getNextItemNetId() {
        return itemNetId.getAndIncrement();
    }

    public void confirmTeleport(Vector3d position) {
        if (unconfirmedTeleport == null) {
            return;
        }

        if (unconfirmedTeleport.canConfirm(position)) {
            unconfirmedTeleport = null;
            return;
        }

        // Resend the teleport every few packets until Bedrock responds
        unconfirmedTeleport.incrementUnconfirmedFor();
        if (unconfirmedTeleport.shouldResend()) {
            unconfirmedTeleport.resetUnconfirmedFor();
            geyser.getLogger().debug("Resending teleport " + unconfirmedTeleport.getTeleportConfirmId());
            getPlayerEntity().moveAbsolute(
                    Vector3f.from(unconfirmedTeleport.getX(), unconfirmedTeleport.getY(), unconfirmedTeleport.getZ()),
                    unconfirmedTeleport.getYaw(), unconfirmedTeleport.getPitch(), getPlayerEntity().isOnGround(), true);
        }
    }

    /**
     * Queue a packet to be sent to player.
     *
     * @param packet the bedrock packet from the NukkitX protocol lib
     */
    public void sendUpstreamPacket(BedrockPacket packet) {
        upstream.sendPacket(packet);
    }

    /**
     * Send a packet immediately to the player.
     *
     * @param packet the bedrock packet from the NukkitX protocol lib
     */
    public void sendUpstreamPacketImmediately(BedrockPacket packet) {
        upstream.sendPacketImmediately(packet);
    }

    /**
     * Send a packet to the remote server if in the game state.
     *
     * @param packet the java edition packet from MCProtocolLib
     */
    public void sendDownstreamGamePacket(Packet packet) {
        sendDownstreamPacket(packet, ProtocolState.GAME);
    }

    /**
     * Send a packet to the remote server if in the login state.
     *
     * @param packet the java edition packet from MCProtocolLib
     */
    public void sendDownstreamLoginPacket(Packet packet) {
        sendDownstreamPacket(packet, ProtocolState.LOGIN);
    }

    /**
     * Send a packet to the remote server if in the specified state.
     *
     * @param packet        the java edition packet from MCProtocolLib
     * @param intendedState the state the client should be in
     */
    public void sendDownstreamPacket(Packet packet, ProtocolState intendedState) {
        // protocol can be null when we're not yet logged in (online auth)
        if (protocol == null) {
            if (geyser.getConfig().isDebugMode()) {
                geyser.getLogger().debug("Tried to send downstream packet with no downstream session!");
                Thread.dumpStack();
            }
            return;
        }

        if (protocol.getState() != intendedState) {
            geyser.getLogger().debug("Tried to send " + packet.getClass().getSimpleName() + " packet while not in "
                    + intendedState.name() + " state");
            return;
        }

        sendDownstreamPacket(packet);
    }

    /**
     * Send a packet to the remote server.
     *
     * @param packet the java edition packet from MCProtocolLib
     */
    public void sendDownstreamPacket(Packet packet) {
        if (!closed && this.downstream != null) {
            Channel channel = this.downstream.getSession().getChannel();
            if (channel == null) {
                // Channel is only null before the connection has initialized
                geyser.getLogger().warning("Tried to send a packet to the Java server too early!");
                if (geyser.getConfig().isDebugMode()) {
                    Thread.dumpStack();
                }
                return;
            }

            EventLoop eventLoop = channel.eventLoop();
            if (eventLoop.inEventLoop()) {
                sendDownstreamPacket0(packet);
            } else {
                eventLoop.execute(() -> sendDownstreamPacket0(packet));
            }
        }
    }

    private void sendDownstreamPacket0(Packet packet) {
        ProtocolState state = protocol.getState();
        if (state == ProtocolState.GAME || state == ProtocolState.CONFIGURATION
                || packet.getClass() == ServerboundCustomQueryAnswerPacket.class) {
            downstream.sendPacket(packet);
        } else {
            geyser.getLogger().debug("Tried to send downstream packet " + packet.getClass().getSimpleName()
                    + " before connected to the server");
        }
    }

    /**
     * Update the cached value for the reduced debug info gamerule.
     * If enabled, also hides the player's coordinates.
     *
     * @param value The new value for reducedDebugInfo
     */
    public void setReducedDebugInfo(boolean value) {
        this.sessionInformation.setReducedDebugInfo(value);
        // Set the showCoordinates data. This is done because updateShowCoordinates()
        // uses this gamerule as a variable.
        preferencesCache.updateShowCoordinates();
    }

    /**
     * Changes the daylight cycle gamerule on the client
     * This is used in the login screen along-side normal usage
     *
     * @param doCycle If the cycle should continue
     */
    public void setDaylightCycle(boolean doCycle) {
        sendGameRule("dodaylightcycle", doCycle);
        // Save the value so we don't have to constantly send a daylight cycle gamerule
        // update
        this.sessionInformation.setDaylightCycle(doCycle);
    }

    /**
     * Send a gamerule value to the client
     *
     * @param gameRule The gamerule to send
     * @param value    The value of the gamerule
     */
    public void sendGameRule(String gameRule, Object value) {
        GameRulesChangedPacket gameRulesChangedPacket = new GameRulesChangedPacket();
        gameRulesChangedPacket.getGameRules().add(new GameRuleData<>(gameRule, value));
        upstream.sendPacket(gameRulesChangedPacket);
    }

    /**
     * Checks if the given session's player has a permission
     *
     * @param permission The permission node to check
     * @return true if the player has the requested permission, false if not
     */
    @Override
    public boolean hasPermission(String permission) {
        return geyser.getWorldManager().hasPermission(this, permission);
    }

    private int getRenderDistance() {
        if (clientRenderDistance != -1) {
            // The client has sent a render distance
            return clientRenderDistance;
        } else if (serverRenderDistance != -1) {
            // only known once ClientboundLoginPacket is received
            return serverRenderDistance;
        }
        return 2; // unfortunate default until we got more info
    }

    // We need to send our skin parts to the server otherwise java sees us with no
    // hat, jacket etc
    private static final List<SkinPart> SKIN_PARTS = Arrays.asList(SkinPart.values());

    /**
     * Send a packet to the server to indicate client render distance, locale, skin
     * parts, and hand preference.
     */
    public void sendJavaClientSettings() {
        ServerboundClientInformationPacket clientSettingsPacket = new ServerboundClientInformationPacket(locale(),
                getRenderDistance(), ChatVisibility.FULL, true, SKIN_PARTS,
                HandPreference.RIGHT_HAND, false, true);
        sendDownstreamPacket(clientSettingsPacket);
    }

    /**
     * Used for updating statistic values since we only get changes from the server
     *
     * @param statistics Updated statistics values
     */
    public void updateStatistics(@NonNull Object2IntMap<Statistic> statistics) {
        if (this.statistics.isEmpty()) {
            // Initialize custom statistics to 0, so that they appear in the form
            for (CustomStatistic customStatistic : CustomStatistic.values()) {
                this.statistics.put(customStatistic, 0);
            }
        }
        this.statistics.putAll(statistics);
    }

    public void refreshEmotes(List<UUID> emotes) {
        this.sessionInformation.refreshEmotes(emotes);
    }

    public void playSoundEvent(SoundEvent sound, Vector3f position) {
        LevelSoundEvent2Packet packet = new LevelSoundEvent2Packet();
        packet.setPosition(position);
        packet.setSound(sound);
        packet.setIdentifier(":");
        packet.setExtraData(-1);
        sendUpstreamPacket(packet);
    }

    public float getEyeHeight() {
        return this.sessionInformation.getEyeHeight();
    }

    @Override
    public @NonNull String bedrockUsername() {
        return authData.name();
    }

    @Override
    public @MonotonicNonNull String javaUsername() {
        return getPlayerEntity().getUsername();
    }

    @Override
    public UUID javaUuid() {
        return getPlayerEntity().getUuid();
    }

    @Override
    public @NonNull String xuid() {
        return authData.xuid();
    }

    @Override
    public @NonNull String version() {
        return clientData.getGameVersion();
    }

    @Override
    public @NonNull BedrockPlatform platform() {
        return BedrockPlatform.values()[clientData.getDeviceOs().ordinal()]; //todo
    }

    @Override
    public @NonNull String languageCode() {
        return locale();
    }

    @Override
    public @NonNull UiProfile uiProfile() {
        return UiProfile.values()[clientData.getUiProfile().ordinal()]; //todo
    }

    @Override
    public @NonNull InputMode inputMode() {
        return InputMode.values()[clientData.getCurrentInputMode().ordinal()]; //todo
    }

    @Override
    public boolean isLinked() {
        return false; //todo
    }

    @SuppressWarnings("ConstantConditions") // Need to enforce the parameter annotations
    @Override
    public boolean transfer(@NonNull String address, @IntRange(from = 0, to = 65535) int port) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Server address cannot be null or blank");
        } else if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Server port must be between 0 and 65535, was " + port);
        }
        TransferPacket transferPacket = new TransferPacket();
        transferPacket.setAddress(address);
        transferPacket.setPort(port);
        sendUpstreamPacket(transferPacket);
        return true;
    }

    @Override
    public @NonNull CompletableFuture<@Nullable GeyserEntity> entityByJavaId(@NonNegative int javaId) {
        CompletableFuture<GeyserEntity> future = new CompletableFuture<>();
        ensureInEventLoop(() -> future.complete(this.entityCache.getEntityByJavaId(javaId)));
        return future;
    }

    @Override
    public void showEmote(@NonNull GeyserPlayerEntity emoter, @NonNull String emoteId) {
        this.sessionInformation.showEmote(emoter, emoteId);
    }

    @Override
    public void shakeCamera(float intensity, float duration, @NonNull CameraShake type) {
        this.sessionInformation.shakeCamera(intensity, duration, type);
    }

    @Override
    public void stopCameraShake() {
        this.sessionInformation.stopCameraShake();
    }

    @Override
    public void sendFog(String... fogNameSpaces) {
        this.sessionInformation.sendFog(fogNameSpaces);
    }

    @Override
    public void removeFog(String... fogNameSpaces) {
        this.sessionInformation.removeFog(fogNameSpaces);
    }

    @Override
    public @NonNull Set<String> fogEffects() {
        return this.sessionInformation.fogEffects();
    }

    public void addCommandEnum(String name, String enums) {
        softEnumPacket(name, SoftEnumUpdateType.ADD, enums);
    }

    public void removeCommandEnum(String name, String enums) {
        softEnumPacket(name, SoftEnumUpdateType.REMOVE, enums);
    }

    private void softEnumPacket(String name, SoftEnumUpdateType type, String enums) {
        // There is no need to send command enums if command suggestions are disabled
        if (!this.geyser.getConfig().isCommandSuggestions()) {
            return;
        }

        UpdateSoftEnumPacket packet = new UpdateSoftEnumPacket();
        packet.setType(type);
        packet.setSoftEnum(new CommandEnumData(name, Collections.singletonMap(enums, Collections.emptySet()), true));
        sendUpstreamPacket(packet);
    }

    public SessionPlayerEntity getPlayerEntity() {
        return this.sessionInformation.getPlayerEntity();
    }

    public GameMode getGameMode() {
        return this.sessionInformation.getGameMode();
    }

    public void setGameMode(GameMode gameMode) {
        this.sessionInformation.setGameMode(gameMode);
    }

    public boolean isDaylightCycle() {
        return this.sessionInformation.isDaylightCycle();
    }

    public Set<UUID> getEmotes() {
        return this.sessionInformation.getEmotes();
    }

    public int getArmAnimationTicks() {
        return this.sessionInformation.getArmAnimationTicks();
    }

    public void setArmAnimationTicks(int armAnimationTicks) {
        this.sessionInformation.setArmAnimationTicks(armAnimationTicks);
    }

    public void armSwingPending() {
        this.sessionInformation.armSwingPending();
    }

    public void activateArmAnimationTicking() {
        this.sessionInformation.activateArmAnimationTicking();
    }

    public PlayerInventory getPlayerInventory() {
        return this.sessionInformation.getPlayerInventory();
    }

    public Inventory getOpenInventory() {
        return this.sessionInformation.getOpenInventory();
    }

    public void setOpenInventory(Inventory inventory) {
        this.sessionInformation.setOpenInventory(inventory);
    }

    public boolean isClosingInventory() {
        return this.sessionInformation.isClosingInventory();
    }

    public void setClosingInventory(boolean closingInventory) {
        this.sessionInformation.setClosingInventory(closingInventory);
    }

    public boolean isAdvancedTooltips() {
        return this.sessionInformation.isAdvancedTooltips();
    }

    public void setAdvancedTooltips(boolean advancedTooltips) {
        this.sessionInformation.setAdvancedTooltips(advancedTooltips);
    }

    public void requestOffhandSwap() {
        this.sessionInformation.requestOffhandSwap();
    }

    public boolean isSneaking() {
        return this.sessionInformation.isSneaking();
    }

    public boolean canUseCommandBlocks() {
        return this.sessionInformation.canUseCommandBlocks();
    }

    public JavaDimension getDimensionType() {
        return this.sessionInformation.getDimensionType();
    }

    public void setDimensionType(JavaDimension dimensionType) {
        this.sessionInformation.setDimensionType(dimensionType);
    }

    public Vector3i getLastInteractionBlockPosition() {
        return this.sessionInformation.getLastInteractionBlockPosition();
    }

    public void setLastInteractionBlockPosition(Vector3i lastInteractionBlockPosition) {
        this.sessionInformation.setLastInteractionBlockPosition(lastInteractionBlockPosition);
    }

    public boolean isSteeringLeft() {
        return this.sessionInformation.isSteeringLeft();
    }

    public void setSteeringLeft(boolean steeringLeft) {
        this.sessionInformation.setSteeringLeft(steeringLeft);
    }

    public boolean isSteeringRight() {
        return this.sessionInformation.isSteeringRight();
    }

    public void setSteeringRight(boolean steeringRight) {
        this.sessionInformation.setSteeringRight(steeringRight);
    }

    public boolean isSpawned() {
        return this.sessionInformation.isSpawned();
    }

    public void setSpawned(boolean spawned) {
        this.sessionInformation.setSpawned(spawned);
    }

    public String getDimension() {
        return this.sessionInformation.getDimension();
    }

    public void setDimension(String dimension) {
        this.sessionInformation.setDimension(dimension);
    }

    public Vector3f getLastInteractionPlayerPosition() {
        return this.sessionInformation.getLastInteractionPlayerPosition();
    }

    public void setLastInteractionPlayerPosition(Vector3f lastInteractionPlayerPosition) {
        this.sessionInformation.setLastInteractionPlayerPosition(lastInteractionPlayerPosition);
    }


    public boolean isInstabuild() {
        return this.sessionInformation.isInstabuild();
    }

    public void setInstabuild(boolean instabuild) {
        this.sessionInformation.setInstabuild(instabuild);
    }

    public Entity getMouseoverEntity() {
        return this.sessionInformation.getMouseoverEntity();
    }

    public void setMouseoverEntity(Entity mouseoverEntity) {
        this.sessionInformation.setMouseoverEntity(mouseoverEntity);
    }

    public double getAttackSpeed() {
        return this.sessionInformation.getAttackSpeed();
    }

    public void setAttackSpeed(double attackSpeed) {
        this.sessionInformation.setAttackSpeed(attackSpeed);
    }

    public float getWalkSpeed() {
        return this.sessionInformation.getWalkSpeed();
    }

    public void setWalkSpeed(float walkSpeed) {
        this.sessionInformation.setWalkSpeed(walkSpeed);
    }

    public long getLastHitTime() {
        return this.sessionInformation.getLastHitTime();
    }

    public void setLastHitTime(long lastHitTime) {
        this.sessionInformation.setLastHitTime(lastHitTime);
    }

    public long getLastInteractionTime() {
        return this.sessionInformation.getLastInteractionTime();
    }

    public void setLastInteractionTime(long lastInteractionTime) {
        this.sessionInformation.setLastInteractionTime(lastInteractionTime);
    }

    public long getBlockBreakStartTime() {
        return this.sessionInformation.getBlockBreakStartTime();
    }

    public void setBlockBreakStartTime(long blockBreakStartTime) {
        this.sessionInformation.setBlockBreakStartTime(blockBreakStartTime);
    }

    public float getFlySpeed() {
        return this.sessionInformation.getFlySpeed();
    }

    public void setFlySpeed(float flySpeed) {
        this.sessionInformation.setFlySpeed(flySpeed);
    }

    public long getLastVehicleMoveTimestamp() {
        return this.sessionInformation.getLastVehicleMoveTimestamp();
    }

    public void setLastVehicleMoveTimestamp(long lastVehicleMoveTimestamp) {
        this.sessionInformation.setLastVehicleMoveTimestamp(lastVehicleMoveTimestamp);
    }

    public boolean isSprinting() {
        return this.sessionInformation.isSprinting();
    }

    public void setSprinting(boolean sprinting) {
        this.sessionInformation.setSprinting(sprinting);
    }

    public void startSneaking() {
        this.sessionInformation.startSneaking();
    }

    public void stopSneaking() {
        this.sessionInformation.stopSneaking();
    }

    public void setSwimming(boolean swimming) {
        this.sessionInformation.setSwimming(swimming);
    }

    public boolean isFlying() {
        return this.sessionInformation.isFlying();
    }

    public void setFlying(boolean flying) {
        this.sessionInformation.setFlying(flying);
    }

    public boolean isCanFly() {
        return this.sessionInformation.isCanFly();
    }

    public void setCanFly(boolean canFly) {
        this.sessionInformation.setCanFly(canFly);
    }

    public int getBreakingBlock() {
        return this.sessionInformation.getBreakingBlock();
    }

    public void setBreakingBlock(int breakingBlock) {
        this.sessionInformation.setBreakingBlock(breakingBlock);
    }

    public void sendAdventureSettings() {
        this.sessionInformation.sendAdventureSettings();
    }

    public void setPose(Pose pose) {
        this.sessionInformation.setPose(pose);
    }

    public void setSwimmingInWater(boolean swimmingInWater) {
        this.sessionInformation.setSwimmingInWater(swimmingInWater);
    }

    public @Nullable AttributeData adjustSpeed() {
        return this.sessionInformation.adjustSpeed();
    }

    public float getOriginalSpeedAttribute() {
        return this.sessionInformation.getOriginalSpeedAttribute();
    }

    public void setOriginalSpeedAttribute(float originalSpeedAttribute) {
        this.sessionInformation.setOriginalSpeedAttribute(originalSpeedAttribute);
    }

    public Vector3i getLastBlockPlacePosition() {
        return this.sessionInformation.getLastBlockPlacePosition();
    }

    public void setLastBlockPlacePosition(Vector3i lastBlockPlacePosition) {
        this.sessionInformation.setLastBlockPlacePosition(lastBlockPlacePosition);
    }

    public String getLastBlockPlacedId() {
        return this.sessionInformation.getLastBlockPlacedId();
    }

    public void setLastBlockPlacedId(String lastBlockPlacedId) {
        this.sessionInformation.setLastBlockPlacedId(lastBlockPlacedId);
    }

    public boolean isReducedDebugInfo() {
        return this.sessionInformation.isReducedDebugInfo();
    }

    public boolean isPlacedBucket() {
        return this.sessionInformation.isPlacedBucket();
    }

    public void setPlacedBucket(boolean placedBucket) {
        this.sessionInformation.setPlacedBucket(placedBucket);
    }

    public int getOpPermissionLevel() {
        return this.sessionInformation.getOpPermissionLevel();
    }

    public void setOpPermissionLevel(int opPermissionLevel) {
        this.sessionInformation.setOpPermissionLevel(opPermissionLevel);
    }

    public boolean isInteracting() {
        return this.sessionInformation.isInteracting();
    }

    public void setInteracting(boolean interacting) {
        this.sessionInformation.setInteracting(interacting);
    }

    public String getWorldName() {
        return this.sessionInformation.getWorldName();
    }

    public void setWorldName(String worldName) {
        this.sessionInformation.setWorldName(worldName);
    }

    public String[] getLevels() {
        return this.sessionInformation.getLevels();
    }

    public void setLevels(String[] levels) {
        this.sessionInformation.setLevels(levels);
    }

    public boolean isSentSpawnPacket() {
        return this.connectionManager.isSentSpawnPacket();
    }

    public long getLastMovementTimestamp() {
        return this.sessionInformation.getLastMovementTimestamp();
    }

    public void setLastMovementTimestamp(long lastMovementTimestamp) {
        this.sessionInformation.setLastMovementTimestamp(lastMovementTimestamp);
    }

    public boolean isInWorldBorderWarningArea() {
        return this.sessionInformation.isInWorldBorderWarningArea();
    }

    public void setInWorldBorderWarningArea(boolean inWorldBorderWarningArea) {
        this.sessionInformation.setInWorldBorderWarningArea(inWorldBorderWarningArea);
    }

}
