package com.example.hideboatsails;

import com.google.inject.Provides;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.GameState;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.game.ObjectManager;

@PluginDescriptor(
        name = "Hide Personal Boat Sail",
        description = "Hides only your boat sail",
        tags = {"boat", "sail", "hide"}
)
public class HideBoatSailsPlugin extends Plugin
{
    private static final int SAIL_OBJECT_ID = 60473;
    private static final int MAX_DISTANCE = 2; // tiles from player

    @Inject
    private Clie
