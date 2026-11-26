/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.List;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

/**
 * Replacement for Fabric API's {@code ScreenUtils.getButtons(Screen)}.
 */
public final class ScreenUtils
{
	private ScreenUtils()
	{
	}

	public static List<AbstractWidget> getButtons(Screen screen)
	{
		return screen.children.stream()
			.filter(AbstractWidget.class::isInstance)
			.map(AbstractWidget.class::cast).toList();
	}
}
