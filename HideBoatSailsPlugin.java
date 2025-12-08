package com.example.hideboatsails;

import com.google.inject.Provides;
import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Tile;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.GameState;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.HashSet;
import java.util.Set;

@PluginDescriptor(
        name = "Hide Boat Sails",
        description = "Hides boat sails when not navigating",
        tags = {"boats", "sails", "hide"}
)
public class HideBoatSailsPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private HideBoatSailsConfig config;

    private final Set<GameObject> hiddenSails = new HashSet<>();

    /**
     * Replace with real sail object IDs using RuneLite dev tools
     */
    private static final int[] SAIL_OBJECT_IDS = {
        // ObjectID.EXAMPLE_SAIL
    };

    @Override
    protected void startUp()
    {
        refreshAllExistingSails();
    }

    @Override
    protected void shutDown()
    {
        restoreAllSails();
    }

    @Provides
    HideBoatSailsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(HideBoatSailsConfig.class);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            refreshAllExistingSails();
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        GameObject obj = event.getGameObject();

        for (int id : SAIL_OBJECT_IDS)
        {
            if (obj.getId() == id)
            {
                handleSail(obj);
                break;
            }
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        hiddenSails.remove(event.getGameObject());
    }

    private void refreshAllExistingSails()
    {
        if (client.getScene() == null)
        {
            return;
        }

        Tile[][][] tiles = client.getScene().getTiles();

        for (Tile[][] plane : tiles)
        {
            for (Tile[] row : plane)
            {
                for (Tile tile : row)
                {
                    if (tile == null)
                    {
                        continue;
                    }

                    for (GameObject obj : tile.getGameObjects())
                    {
                        if (obj == null)
                        {
                            continue;
                        }

                        for (int id : SAIL_OBJECT_IDS)
                        {
                            if (obj.getId() == id)
                            {
                                handleSail(obj);
                            }
                        }
                    }
                }
            }
        }
    }

    private void handleSail(GameObject sail)
    {
        if (!isNavigating())
        {
            hideSail(sail);
        }
        else
        {
            showSail(sail);
        }
    }

    private void hideSail(GameObject sail)
    {
        if (sail.getRenderable() != null)
        {
            sail.setRenderable(null);
            hiddenSails.add(sail);
        }
    }

    private void showSail(GameObject sail)
    {
        sail.setRenderable(sail.getRenderable());
        hiddenSails.remove(sail);
    }

    private void restoreAllSails()
    {
        for (GameObject sail : hiddenSails)
        {
            sail.setRenderable(sail.getRenderable());
        }
        hiddenSails.clear();
    }

    private boolean isNavigating()
    {
        return false;
    }
}
