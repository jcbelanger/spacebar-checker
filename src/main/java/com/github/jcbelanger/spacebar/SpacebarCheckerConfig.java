package com.github.jcbelanger.spacebar;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("spacebarchecker")
public interface SpacebarCheckerConfig extends Config
{
	@ConfigItem(
			keyName = "minRun",
			name = "Minimum Run Start",
			description = "Number of spacebars messages before a streak will start"
	)
	default int minRunStart()
	{
		return 3;
	}

	@ConfigItem(
			keyName = "isNotifyOnStart",
			name = "Notify On Start",
			description = "Enable a notification when a spacebar check starts"
	)
	default boolean isNotifyOnStart()
	{
		return true;
	}

	@ConfigItem(
			keyName = "isDuplicatesAllowed",
			name = "Duplicates Allowed",
			description = "Allow a single person to submit multiple spacebar messages per run"
	)
	default boolean isDuplicatesAllowed() { return false; }
}
