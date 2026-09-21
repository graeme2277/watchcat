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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

class WatchcatHealthOverlay extends Overlay
{
	private static final int TEXT_HEIGHT_OFFSET = 28;
	private static final int OVERHEAD_TEXT_EXTRA_OFFSET = 24;
	private static final long FLASH_INTERVAL_MILLIS = 400L;
	private static final Color HEALTH_COLOR = new Color(255, 255, 255);
	private static final Color DANGER_COLOR = new Color(255, 40, 40);

	private final WatchcatPlugin plugin;
	private final WatchcatConfig config;

	@Inject
	WatchcatHealthOverlay(WatchcatPlugin plugin, WatchcatConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		NPC cat = plugin.getFightingCat();
		if (cat == null)
		{
			return null;
		}

		int maxHealth = WatchcatPlugin.getMaxHealth(cat.getId());
		int health = WatchcatPlugin.getCurrentHealth(cat, maxHealth);
		if (health < 0)
		{
			return null;
		}

		boolean danger = health * 100 <= maxHealth * config.flashThreshold();
		boolean flashVisible = (System.currentTimeMillis() / FLASH_INTERVAL_MILLIS) % 2 == 0;
		String text = danger && flashVisible
			? "!  " + health + " / " + maxHealth + "  !"
			: health + " / " + maxHealth;

		graphics.setFont(FontManager.getRunescapeSmallFont());
		int heightOffset = cat.getLogicalHeight() + TEXT_HEIGHT_OFFSET;
		if (cat.getOverheadText() != null)
		{
			// The cat says "Meow" when fed. Move the health text above the
			// overhead message so it does not overlap the health bar or message.
			heightOffset += OVERHEAD_TEXT_EXTRA_OFFSET;
		}
		Point textLocation = cat.getCanvasTextLocation(graphics, text, heightOffset);
		if (textLocation != null)
		{
			Color color = danger && flashVisible ? DANGER_COLOR : HEALTH_COLOR;
			OverlayUtil.renderTextLocation(graphics, textLocation, text, color);
		}

		return null;
	}
}
