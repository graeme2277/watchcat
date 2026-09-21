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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.TileObject;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

class WatchcatSpiceOverlay extends Overlay
{
	private static final Color RED_SPICE = new Color(210, 40, 40);
	private static final Color ORANGE_SPICE = new Color(255, 135, 0);
	private static final Color YELLOW_SPICE = new Color(255, 215, 0);
	private static final Color BROWN_SPICE = new Color(140, 90, 40);

	private final Client client;
	private final WatchcatConfig config;
	private final WatchcatPlugin plugin;

	@Inject
	WatchcatSpiceOverlay(Client client, WatchcatConfig config, WatchcatPlugin plugin)
	{
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.highlightSpiceTiles() || !plugin.isInBasement())
		{
			return null;
		}

		for (TileObject spiceObject : plugin.getSpiceObjects())
		{
			Polygon polygon = Perspective.getCanvasTilePoly(client, spiceObject.getLocalLocation());
			if (polygon == null)
			{
				continue;
			}

			Color color = getSpiceColor(spiceObject.getId());
			Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 70);
			OverlayUtil.renderPolygon(graphics, polygon, color, fillColor, new BasicStroke(2));
		}
		return null;
	}

	private static Color getSpiceColor(int objectId)
	{
		switch (objectId)
		{
			case ObjectID._100_DAVE_SPICE_RED:
				return RED_SPICE;
			case ObjectID._100_DAVE_SPICE_ORANGE:
				return ORANGE_SPICE;
			case ObjectID._100_DAVE_SPICE_BROWN:
				return BROWN_SPICE;
			case ObjectID._100_DAVE_SPICE_YELLOW:
			default:
				return YELLOW_SPICE;
		}
	}
}
