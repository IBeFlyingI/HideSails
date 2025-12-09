package com.example.hideboatsails;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Collection;

import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.api.Perspective;

public class HideSailOverlay extends Overlay
{
    private final Client client;
    private final HideBoatSailsPlugin plugin;
    private final HideBoatSailsConfig config;
    private static final Color HIDE_COLOR = new Color(0, 0, 0, 255); // solid black cover

    @Inject
    public HideSailOverlay(Client client, HideBoatSailsPlugin plugin, HideBoatSailsConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        if (!plugin.isSalvaging())
        {
            return null;
        }

        Collection<GameObject> objects;
        try
        {
            objects = client.getGameObjects();
        }
        catch (NoSuchMethodError | AbstractMethodError e)
        {
            // Fallback: client.getGameObjects may not be present — nothing we can do here
            return null;
        }

        if (objects == null || objects.isEmpty())
        {
            return null;
        }

        int sailId = config.sailObjectId();

        for (GameObject obj : objects)
        {
            if (obj == null)
            {
                continue;
            }
            if (obj.getId() != sailId)
            {
                continue;
            }

            LocalPoint lp = obj.getLocalLocation();
            if (lp == null)
            {
                continue;
            }

            // Only draw if near player (same logic as plugin)
            if (!plugin.isNearPlayer(lp))
            {
                continue;
            }

            // Get tile polygon for that local point and draw a filled polygon to cover the sail visually
            Polygon poly = Perspective.getCanvasTilePoly(client, lp, obj.getPlane());
            if (poly != null)
            {
                OverlayUtil.renderPolygon(g, poly, HIDE_COLOR);
                // Optionally draw a border (transparent) or nothing — we only need to cover the object
            }
        }

        return null;
    }
}