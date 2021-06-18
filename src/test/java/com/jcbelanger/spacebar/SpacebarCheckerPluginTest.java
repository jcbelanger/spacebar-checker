package com.jcbelanger.spacebar;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SpacebarCheckerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SpacebarCheckerPlugin.class);
		RuneLite.main(args);
	}
}