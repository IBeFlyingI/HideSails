package com.example.hideboatsails;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("hideboatsails")
public interface HideBoatSailsConfig extends Config
{
    @ConfigItem(
            keyName = "sailObjectId",
            name = "Sail Object ID",
            description = "ID of the sail object to hide",
            position = 1
    )
    default int sailObjectId()
    {
        return 60473;
    }

    @ConfigItem(
            keyName = "hideDurationSeconds",
            name = "Salvaging timeout (s)",
            description = "How many seconds to keep hiding after a salvage click if no animation is detected. Extended while animation continues.",
            position = 2
    )
    default int hideDurationSeconds()
    {
        return 6;
    }

    @ConfigItem(
            keyName = "debug",
            name = "Enable debug logging",
            description = "Enable extra debug log messages for troubleshooting",
            position = 3
    )
    default boolean debug()
    {
        return false;
    }
}