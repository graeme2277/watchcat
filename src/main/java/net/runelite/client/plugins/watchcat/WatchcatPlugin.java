/*
 * Copyright (c) 2026, Graeme
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.watchcat;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Watchcat",
	description = "Protects your cat while it fights a Hell-Rat Behemoth",
	tags = {"cat", "hell-rat", "behemoth", "health", "edgeville", "food"}
)
public class WatchcatPlugin extends Plugin
{
	private static final int CRITICAL_HEALTH = 2;
	private static final long SCREEN_FLASH_DURATION_MILLIS = 4_000L;
	private static final String INSERT_CAT_PROMPT = "Insert your cat";
	private static final Set<Integer> CAT_FOOD = ImmutableSet.of(
		ItemID.SHRIMP, ItemID.RAW_SHRIMP,
		ItemID.ANCHOVIES, ItemID.RAW_ANCHOVIES,
		ItemID.SARDINE, ItemID.RAW_SARDINE,
		ItemID.HERRING, ItemID.RAW_HERRING,
		ItemID.MACKEREL, ItemID.RAW_MACKEREL,
		ItemID.TROUT, ItemID.RAW_TROUT,
		ItemID.COD, ItemID.RAW_COD,
		ItemID.PIKE, ItemID.RAW_PIKE,
		ItemID.SALMON, ItemID.RAW_SALMON,
		ItemID.TUNA, ItemID.RAW_TUNA,
		ItemID.LOBSTER, ItemID.RAW_LOBSTER,
		ItemID.BASS, ItemID.RAW_BASS,
		ItemID.SWORDFISH, ItemID.RAW_SWORDFISH,
		ItemID.SHARK, ItemID.RAW_SHARK,
		ItemID.SEATURTLE, ItemID.RAW_SEATURTLE,
		ItemID.MANTARAY, ItemID.RAW_MANTARAY,
		ItemID.MONKFISH, ItemID.RAW_MONKFISH,
		ItemID.ANGLERFISH, ItemID.RAW_ANGLERFISH,
		ItemID.DARK_CRAB, ItemID.RAW_DARK_CRAB,
		ItemID.HUNTING_FISH_SPECIAL, ItemID.HUNTING_RAW_FISH_SPECIAL,
		ItemID.TBWT_RAW_KARAMBWAN, ItemID.TBWT_COOKED_KARAMBWAN,
		ItemID.TBWT_POORLY_COOKED_KARAMBWAN, ItemID.TBWT_RAW_KARAMBWANJI,
		ItemID.TBWT_COOKED_KARAMBWANJI, ItemID.BLIGHTED_KARAMBWAN,
		ItemID.BLIGHTED_MANTARAY, ItemID.BLIGHTED_ANGLERFISH,
		ItemID.CAVE_EEL, ItemID.BRUT_ROE, ItemID.BRUT_CAVIAR,
		ItemID.GIANT_FROGSPAWN, ItemID.BUCKET_MILK, ItemID.BOTTOMLESS_MILK_BUCKET_FILLED);

	@Inject
	private Client client;

	@Inject
	private Notifier notifier;

	@Inject
	private WatchcatConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private WatchcatHealthOverlay overlay;

	@Inject
	private WatchcatAlertOverlay alertOverlay;

	private boolean criticalAlertSent;
	private boolean insertCatPromptVisible;
	private long screenFlashUntil;

	@Provides
	WatchcatConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WatchcatConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		overlayManager.add(alertOverlay);
		criticalAlertSent = false;
		insertCatPromptVisible = false;
		screenFlashUntil = 0;
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(alertOverlay);
		criticalAlertSent = false;
		insertCatPromptVisible = false;
		screenFlashUntil = 0;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		boolean promptVisible = containsText(
			client.getWidget(InterfaceID.Chatmenu.UNIVERSE), INSERT_CAT_PROMPT);
		if (promptVisible && !insertCatPromptVisible && config.noFoodAlert() && !hasCatFood())
		{
			screenFlashUntil = System.currentTimeMillis() + SCREEN_FLASH_DURATION_MILLIS;
		}
		insertCatPromptVisible = promptVisible;

		NPC cat = getFightingCat();
		if (cat == null)
		{
			criticalAlertSent = false;
			return;
		}

		int maxHealth = getMaxHealth(cat.getId());
		int health = getCurrentHealth(cat, maxHealth);
		if (health > CRITICAL_HEALTH)
		{
			criticalAlertSent = false;
			return;
		}

		if (health > 0 && config.criticalAlert() && !criticalAlertSent)
		{
			notifier.notify("Your cat has 2 hitpoints or fewer remaining! Feed it now!");
			screenFlashUntil = System.currentTimeMillis() + SCREEN_FLASH_DURATION_MILLIS;
			criticalAlertSent = true;
		}
	}

	boolean isScreenFlashActive()
	{
		return System.currentTimeMillis() < screenFlashUntil;
	}

	private boolean hasCatFood()
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			return false;
		}

		for (Item item : inventory.getItems())
		{
			if (CAT_FOOD.contains(item.getId()))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean containsText(Widget widget, String expectedText)
	{
		if (widget == null || widget.isHidden())
		{
			return false;
		}

		if (Text.standardize(widget.getText()).contains(Text.standardize(expectedText)))
		{
			return true;
		}

		Widget[] children = widget.getChildren();
		if (children != null)
		{
			for (Widget child : children)
			{
				if (containsText(child, expectedText))
				{
					return true;
				}
			}
		}
		return false;
	}

	NPC getFightingCat()
	{
		NPC cat = client.getFollower();
		if (cat == null || getMaxHealth(cat.getId()) < 0 || cat.getHealthScale() <= 0)
		{
			return null;
		}

		for (NPC npc : cat.getWorldView().npcs())
		{
			if (npc.getId() != NpcID.HUNDRED_DAVE_BIGRAT)
			{
				continue;
			}

			Actor catTarget = cat.getInteracting();
			Actor ratTarget = npc.getInteracting();
			if (catTarget == npc || ratTarget == cat)
			{
				return cat;
			}
		}

		return null;
	}

	static int getMaxHealth(int npcId)
	{
		if (npcId >= NpcID.GROWNCAT && npcId <= NpcID.GROWNCAT_HELL)
		{
			return 6;
		}
		if (npcId >= NpcID.LAZYCAT_LIGHT && npcId <= NpcID.LAZYCAT_HELL)
		{
			return 14;
		}
		if (npcId >= NpcID.WILEYCAT_LIGHT && npcId <= NpcID.WILEYCAT_HELL)
		{
			return 12;
		}
		if (npcId >= NpcID.KITTENPET1 && npcId <= NpcID.KITTENPET_HELL)
		{
			return 4;
		}
		if (npcId >= NpcID.OVERGROWNCAT && npcId <= NpcID.OVERGROWNCAT_HELL)
		{
			return 10;
		}

		return -1;
	}

	static int getCurrentHealth(NPC cat, int maxHealth)
	{
		int ratio = cat.getHealthRatio();
		int scale = cat.getHealthScale();
		if (ratio < 0 || scale <= 0)
		{
			return -1;
		}
		if (ratio == 0)
		{
			return 0;
		}

		// Reverse the server's health-ratio calculation. Cat health is lower than
		// the health-bar scale, so this recovers its exact hitpoints.
		int minimum = ratio > 1
			? (maxHealth * (ratio - 1) + scale - 2) / (scale - 1)
			: 1;
		int maximum = scale > 1
			? (maxHealth * ratio - 1) / (scale - 1)
			: maxHealth;
		maximum = Math.min(maximum, maxHealth);
		return (minimum + maximum + 1) / 2;
	}
}
