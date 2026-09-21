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

import com.google.common.collect.ImmutableMap;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
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
	private static final Map<WorldPoint, Color> SPICE_TILES = ImmutableMap.of(
		new WorldPoint(3078, 9901, 0), RED_SPICE,
		new WorldPoint(3069, 9895, 0), ORANGE_SPICE,
		new WorldPoint(3070, 9881, 0), YELLOW_SPICE,
		new WorldPoint(3081, 9875, 0), BROWN_SPICE);

	private final Client client;
	private final WatchcatConfig config;

	@Inject
	WatchcatSpiceOverlay(Client client, WatchcatConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.highlightSpiceTiles())
		{
			return null;
		}

		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return null;
		}

		for (Map.Entry<WorldPoint, Color> spiceTile : SPICE_TILES.entrySet())
		{
			LocalPoint localPoint = LocalPoint.fromWorld(worldView, spiceTile.getKey());
			if (localPoint == null)
			{
				continue;
			}

			Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
			if (polygon == null)
			{
				continue;
			}

			Color color = spiceTile.getValue();
			Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 70);
			OverlayUtil.renderPolygon(graphics, polygon, color, fillColor, new BasicStroke(2));
		}
		return null;
	}
}
