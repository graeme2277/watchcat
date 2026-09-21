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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("watchcat")
public interface WatchcatConfig extends Config
{
	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "flashThreshold",
		name = "Flash threshold (%)",
		description = "Flash the warning when the cat's health is at or below this percentage"
	)
	default int flashThreshold()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "criticalAlert",
		name = "2 HP critical alert",
		description = "Send a notification and flash the screen when the cat reaches 2 hitpoints"
	)
	default boolean criticalAlert()
	{
		return true;
	}

	@ConfigItem(
		keyName = "noFoodAlert",
		name = "No food alert",
		description = "Show an overlay warning and flash the screen when starting a fight without cat food"
	)
	default boolean noFoodAlert()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightSpiceTiles",
		name = "Highlight spice tiles",
		description = "Highlight each Behemoth tile with the colour of the spice it guards"
	)
	default boolean highlightSpiceTiles()
	{
		return true;
	}
}
