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
package com.graeme2277.watchcat;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
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
	private static final int BASEMENT_WEST_REGION_ID = 12186;
	private static final int BASEMENT_EAST_REGION_ID = 12442;
	private static final int CRITICAL_HEALTH = 2;
	private static final long SCREEN_FLASH_DURATION_MILLIS = 4_000L;
	private static final String INSERT_PROMPT_PREFIX = "insert your";
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
	private ClientThread clientThread;

	@Inject
	private WatchcatConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private WatchcatHealthOverlay overlay;

	@Inject
	private WatchcatAlertOverlay alertOverlay;

	@Inject
	private WatchcatNoFoodOverlay noFoodOverlay;

	@Inject
	private WatchcatSpiceOverlay spiceOverlay;

	private final Set<TileObject> spiceObjects = new HashSet<>();
	private boolean criticalAlertSent;
	private boolean insertCatPromptVisible;
	private boolean inBasementLastTick;
	private long screenFlashUntil;
	private long noFoodWarningUntil;

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
		overlayManager.add(noFoodOverlay);
		overlayManager.add(spiceOverlay);
		clientThread.invokeLater(this::scanSpiceObjects);
		criticalAlertSent = false;
		insertCatPromptVisible = false;
		inBasementLastTick = false;
		screenFlashUntil = 0;
		noFoodWarningUntil = 0;
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(alertOverlay);
		overlayManager.remove(noFoodOverlay);
		overlayManager.remove(spiceOverlay);
		spiceObjects.clear();
		criticalAlertSent = false;
		insertCatPromptVisible = false;
		inBasementLastTick = false;
		screenFlashUntil = 0;
		noFoodWarningUntil = 0;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			spiceObjects.clear();
			inBasementLastTick = false;
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		addSpiceObject(event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		spiceObjects.remove(event.getGameObject());
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		addSpiceObject(event.getGroundObject());
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		spiceObjects.remove(event.getGroundObject());
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		addSpiceObject(event.getDecorativeObject());
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		spiceObjects.remove(event.getDecorativeObject());
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event)
	{
		addSpiceObject(event.getWallObject());
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned event)
	{
		spiceObjects.remove(event.getWallObject());
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		boolean inBasement = isInBasement();
		if (!inBasement)
		{
			spiceObjects.clear();
			criticalAlertSent = false;
			insertCatPromptVisible = false;
			inBasementLastTick = false;
			screenFlashUntil = 0;
			noFoodWarningUntil = 0;
			return;
		}

		if (!inBasementLastTick)
		{
			scanSpiceObjects();
			inBasementLastTick = true;
		}

		boolean promptVisible = containsInsertCatPrompt(
			client.getWidget(InterfaceID.Chatmenu.UNIVERSE))
			|| containsInsertCatPrompt(client.getWidget(InterfaceID.Chatmenu.OPTIONS));
		if (promptVisible && !insertCatPromptVisible && config.noFoodAlert() && !hasCatFood())
		{
			long warningEnd = System.currentTimeMillis() + SCREEN_FLASH_DURATION_MILLIS;
			screenFlashUntil = warningEnd;
			noFoodWarningUntil = warningEnd;
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

	boolean isInBasement()
	{
		if (client.getLocalPlayer() == null)
		{
			return false;
		}

		int regionId = client.getLocalPlayer().getWorldLocation().getRegionID();
		return regionId == BASEMENT_WEST_REGION_ID || regionId == BASEMENT_EAST_REGION_ID;
	}

	Set<TileObject> getSpiceObjects()
	{
		return Collections.unmodifiableSet(spiceObjects);
	}

	private void addSpiceObject(TileObject tileObject)
	{
		if (isInBasement() && tileObject != null && isSpiceObject(tileObject.getId()))
		{
			spiceObjects.add(tileObject);
		}
	}

	private void scanSpiceObjects()
	{
		spiceObjects.clear();
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return;
		}

		for (Tile[][] plane : worldView.getScene().getTiles())
		{
			for (Tile[] column : plane)
			{
				for (Tile tile : column)
				{
					if (tile == null)
					{
						continue;
					}
					addSpiceObject(tile.getGroundObject());
					addSpiceObject(tile.getDecorativeObject());
					addSpiceObject(tile.getWallObject());
					for (GameObject gameObject : tile.getGameObjects())
					{
						addSpiceObject(gameObject);
					}
				}
			}
		}
	}

	static boolean isSpiceObject(int objectId)
	{
		return objectId >= ObjectID._100_DAVE_SPICE_RED
			&& objectId <= ObjectID._100_DAVE_SPICE_YELLOW;
	}

	boolean isScreenFlashActive()
	{
		return isInBasement() && System.currentTimeMillis() < screenFlashUntil;
	}

	boolean isNoFoodWarningActive()
	{
		return isInBasement() && config.noFoodAlert()
			&& System.currentTimeMillis() < noFoodWarningUntil;
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

	private static boolean containsInsertCatPrompt(Widget widget)
	{
		if (widget == null)
		{
			return false;
		}

		String text = Text.standardize(widget.getText());
		if (text.contains(INSERT_PROMPT_PREFIX) && text.contains("cat"))
		{
			return true;
		}

		Widget[] children = widget.getChildren();
		if (children != null)
		{
			for (Widget child : children)
			{
				if (containsInsertCatPrompt(child))
				{
					return true;
				}
			}
		}
		return false;
	}

	NPC getFightingCat()
	{
		if (!isInBasement())
		{
			return null;
		}

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
