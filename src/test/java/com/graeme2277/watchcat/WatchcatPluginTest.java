package com.graeme2277.watchcat;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class WatchcatPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(WatchcatPlugin.class);
		RuneLite.main(args);
	}
}
