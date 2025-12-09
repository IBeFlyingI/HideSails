package com.example.hideboatsails;

import com.google.inject.Provides;
import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.GameState;
import net.runelite.api.Player;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.game.ObjectManager;
import net.runelite.client.ui.overlay.OverlayManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
        name = "Hide Personal Boat Sail",
        description = "Hides only your boat sail visually while salvaging",
        tags = {"boat", "sail", "hide", "salvage"}
)
public class HideBoatSailsPlugin extends Plugin
{
    private static final Logger log = LoggerFactory.getLogger(HideBoatSailsPlugin.class);

    private static final int LOCAL_UNITS_PER_TILE = 128;
    private static final int MAX_DISTANCE = 2; // tiles from player

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private HideSailOverlay overlay;

    @Inject
    private HideBoatSailsConfig config;

    // Whether the plugin is currently in automatic "salvaging" mode
    private volatile boolean salvaging = false;

    // expiry timestamp (ms) for salvaging mode; extended while player animation continues
    private volatile long salvageExpiry = 0L;

    // remember handled objects to avoid repeated work (not strictly required for overlay, but kept for events)
    private final Set<Integer> handled = new HashSet<>();

    @Provides
    HideBoatSailsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(HideBoatSailsConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        handled.clear();
        overlayManager.add(overlay);
        log.info("Hide Personal Boat Sail starting up");
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        handled.clear();
        salvaging = false;
        salvageExpiry = 0L;
        log.info("Hide Personal Boat Sail shutting down");
    }

    public boolean isSalvaging()
    {
        return salvaging;
    }

    // Exposed helper used by overlay to test proximity by LocalPoint
    public boolean isNearPlayer(LocalPoint lp)
    {
        if (client == null || client.getLocalPlayer() == null || lp == null)
        {
            return false;
        }
        LocalPoint playerLp = client.getLocalPlayer().getLocalLocation();
        if (playerLp == null)
        {
            return false;
        }
        int dx = Math.abs(playerLp.getX() - lp.getX());
        int dy = Math.abs(playerLp.getY() - lp.getY());
        int tileDx = dx / LOCAL_UNITS_PER_TILE;
        int tileDy = dy / LOCAL_UNITS_PER_TILE;
        return tileDx <= MAX_DISTANCE && tileDy <= MAX_DISTANCE;
    }

    // Overload used in previous code paths
    public boolean isNearPlayer(GameObject obj)
    {
        if (obj == null)
        {
            return false;
        }
        return isNearPlayer(obj.getLocalLocation());
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        String opt = event.getMenuOption();
        if (opt == null)
        {
            return;
        }

        // Detect salvage menu clicks (case-insensitive). If the user clicked a "Salvage" option we enter salvaging mode.
        if (opt.toLowerCase().contains("salvage"))
        {
            int durationSec = config.hideDurationSeconds();
            salvageExpiry = System.currentTimeMillis() + (durationSec * 1000L);
            salvaging = true;
            if (config.debug())
            {
                log.debug("MenuOptionClicked detected salvage action (option='{}'), entering salvaging mode for {}s", opt, durationSec);
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (!salvaging)
        {
            return;
        }

        Player local = client.getLocalPlayer();
        if (local == null)
        {
            // player not loaded — keep expiry but don't update
            if (config.debug())
            {
                log.debug("GameTick: local player null while salvaging");
            }
        }
        else
        {
            int anim = local.getAnimation();
            long now = System.currentTimeMillis();

            if (anim != -1)
            {
                // While animation present, extend expiry so we keep hiding during animation
                salvageExpiry = now + (config.hideDurationSeconds() * 1000L);
                if (config.debug())
                {
                    log.debug("GameTick: animation={} extending salvage expiry to {}", anim, salvageExpiry);
                }
            }
            // If no animation and expiry elapsed, stop salvaging
            if (now > salvageExpiry)
            {
                salvaging = false;
                if (config.debug())
                {
                    log.debug("GameTick: salvage expiry reached; disabling salvaging mode");
                }
            }
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        // keep handled set updated; overlay reads client.getGameObjects directly
        GameObject obj = event.getGameObject();
        if (obj == null)
        {
            return;
        }
        int key = makeObjectKey(obj);
        handled.add(key);

        if (config.debug())
        {
            log.debug("GameObjectSpawned id={} loc={} key={}", obj.getId(), obj.getLocalLocation(), key);
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        // When logging in, clear handled set so initial scans work
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            if (config.debug())
            {
                log.debug("GameState.LOGGED_IN: clearing handled set");
            }
            handled.clear();
        }
    }

    private int makeObjectKey(GameObject obj)
    {
        LocalPoint lp = obj.getLocalLocation();
        int locHash = lp != null ? (lp.getX() * 31 + lp.getY()) : 0;
        return obj.getId() * 31 + locHash;
    }
}