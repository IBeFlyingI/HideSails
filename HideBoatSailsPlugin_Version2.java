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
import net.runelite.api.GameState;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.game.ObjectManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HideBoatSailsPlugin
 *
 * Attempts to hide the player's nearby boat sail object. Uses reflection-based fallbacks
 * for object removal so the same code is more tolerant of RuneLite API differences.
 *
 * Notes:
 * - Verify the sail object ID (SAIL_OBJECT_ID) for your client version.
 * - Reflection calls may need to be adapted if your ObjectManager implementation differs.
 */
@PluginDescriptor(
        name = "Hide Personal Boat Sail",
        description = "Hides only your boat sail",
        tags = {"boat", "sail", "hide"}
)
public class HideBoatSailsPlugin extends Plugin
{
    private static final Logger log = LoggerFactory.getLogger(HideBoatSailsPlugin.class);

    // Default sail object id used previously — confirm in your client during testing
    private static final int SAIL_OBJECT_ID = 60473;
    private static final int MAX_DISTANCE = 2; // tiles from player
    private static final int LOCAL_UNITS_PER_TILE = 128;

    @Inject
    private Client client;

    @Inject
    private ObjectManager objectManager;

    // remember handled objects to avoid repeated work
    private final Set<Integer> handled = new HashSet<>();

    @Override
    protected void startUp() throws Exception
    {
        handled.clear();
        log.info("Hide Personal Boat Sail starting up");
    }

    @Override
    protected void shutDown() throws Exception
    {
        handled.clear();
        log.info("Hide Personal Boat Sail shutting down");
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        GameObject obj = event.getGameObject();
        if (obj == null)
        {
            return;
        }

        if (obj.getId() == SAIL_OBJECT_ID && isNearPlayer(obj))
        {
            int key = makeObjectKey(obj);
            if (handled.contains(key))
            {
                return;
            }

            log.debug("Detected sail spawn id={} loc={} key={}", obj.getId(), obj.getLocalLocation(), key);

            boolean removed = tryRemoveObject(obj);

            if (removed)
            {
                log.info("Removed sail object id={} at {}", obj.getId(), obj.getLocalLocation());
            }
            else
            {
                log.warn("Failed to remove sail object via known ObjectManager methods; object id={} at {} will still be present visually", obj.getId(), obj.getLocalLocation());
            }

            handled.add(key);
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        // When logging in, scan currently-present objects (best-effort)
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            log.debug("GameState.LOGGED_IN: scanning existing objects for sails");
            try
            {
                // try client.getGameObjects() if present
                try
                {
                    // This call may not exist in every API; wrap in reflection to be safe
                    Method getGameObjects = client.getClass().getMethod("getGameObjects");
                    Object result = getGameObjects.invoke(client);
                    if (result instanceof Iterable)
                    {
                        for (Object o : (Iterable<?>) result)
                        {
                            if (o instanceof GameObject)
                            {
                                GameObject go = (GameObject) o;
                                if (go.getId() == SAIL_OBJECT_ID && isNearPlayer(go))
                                {
                                    int key = makeObjectKey(go);
                                    if (!handled.contains(key))
                                    {
                                        boolean removed = tryRemoveObject(go);
                                        if (removed)
                                        {
                                            log.info("Removed sail from initial scan id={} loc={}", go.getId(), go.getLocalLocation());
                                        }
                                        handled.add(key);
                                    }
                                }
                            }
                        }
                    }
                }
                catch (NoSuchMethodException nsme)
                {
                    // ignore—client.getGameObjects not present; rely on spawn events
                    log.debug("client.getGameObjects() not available on this client build; skipping initial enumeration");
                }
            }
            catch (Exception e)
            {
                log.warn("Error while scanning objects on login", e);
            }
        }
    }

    private boolean tryRemoveObject(GameObject obj)
    {
        // Try a few method names on ObjectManager via reflection in case API differs:
        // - removeObject(GameObject)
        // - removeGameObject(GameObject)
        // - removeObject(int objectId, LocalPoint location, int type?)  <-- fallback
        // This is all best-effort. If none succeed, we return false.
        Object mgr = objectManager;
        if (mgr == null)
        {
            log.warn("ObjectManager is null; cannot remove objects");
            return false;
        }

        Class<?> mgrCls = mgr.getClass();

        // Try removeObject(GameObject)
        try
        {
            Method m = mgrCls.getMethod("removeObject", GameObject.class);
            m.setAccessible(true);
            m.invoke(mgr, obj);
            return true;
        }
        catch (NoSuchMethodException ignored) { }
        catch (Exception e) { log.debug("removeObject(GameObject) invocation failed", e); }

        // Try removeGameObject(GameObject)
        try
        {
            Method m = mgrCls.getMethod("removeGameObject", GameObject.class);
            m.setAccessible(true);
            m.invoke(mgr, obj);
            return true;
        }
        catch (NoSuchMethodException ignored) { }
        catch (Exception e) { log.debug("removeGameObject(GameObject) invocation failed", e); }

        // Try removeObject(int id, LocalPoint loc)
        try
        {
            Method m = mgrCls.getMethod("removeObject", int.class, LocalPoint.class);
            m.setAccessible(true);
            m.invoke(mgr, obj.getId(), obj.getLocalLocation());
            return true;
        }
        catch (NoSuchMethodException ignored) { }
        catch (Exception e) { log.debug("removeObject(int, LocalPoint) invocation failed", e); }

        // Try removeObject(int sceneX, int sceneY, int id) - many permutations exist; try common signatures
        try
        {
            Method[] methods = mgrCls.getMethods();
            for (Method m : methods)
            {
                String name = m.getName().toLowerCase();
                if ((name.contains("remove") || name.contains("set")) && m.getParameterCount() > 0)
                {
                    // this is an exploratory attempt; avoid calling randomly
                    // skip heavy attempts to avoid side effects
                }
            }
        }
        catch (Exception e)
        {
            log.debug("Fallback exploration failed", e);
        }

        return false;
    }

    // Helper: check whether the object is within MAX_DISTANCE tiles of the local player.
    private boolean isNearPlayer(GameObject obj)
    {
        if (client == null || client.getLocalPlayer() == null)
        {
            return false;
        }

        LocalPoint playerLp = client.getLocalPlayer().getLocalLocation();
        LocalPoint objLp = obj.getLocalLocation();
        if (playerLp == null || objLp == null)
        {
            return false;
        }

        int dx = Math.abs(playerLp.getX() - objLp.getX());
        int dy = Math.abs(playerLp.getY() - objLp.getY());

        int tileDx = dx / LOCAL_UNITS_PER_TILE;
        int tileDy = dy / LOCAL_UNITS_PER_TILE;

        return tileDx <= MAX_DISTANCE && tileDy <= MAX_DISTANCE;
    }

    private int makeObjectKey(GameObject obj)
    {
        LocalPoint lp = obj.getLocalLocation();
        int locHash = lp != null ? (lp.getX() * 31 + lp.getY()) : 0;
        return obj.getId() * 31 + locHash;
    }
}