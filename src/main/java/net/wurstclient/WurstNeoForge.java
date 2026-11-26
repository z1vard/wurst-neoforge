/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge entry point. Replaces Fabric's {@code WurstInitializer}.
 *
 * <p>
 * Wurst has to be initialized while {@code Minecraft} is being constructed,
 * otherwise the mixins that run during the loading screen (text rendering,
 * {@code Screen.init()}, ...) would see a null {@code HackList}. NeoForge
 * fires {@link RegisterClientReloadListenersEvent} from
 * {@code Minecraft.<init>}, right after the window, options and resource
 * manager exist but before the loading screen is created, which is the
 * closest equivalent to Fabric's client entrypoint.
 *
 * <p>
 * The client tick listener is kept as a fallback in case the event above
 * never fires.
 */
@Mod("wurst")
public final class WurstNeoForge
{
	private static boolean initialized;

	public WurstNeoForge(IEventBus modEventBus)
	{
		if(FMLEnvironment.dist != Dist.CLIENT)
			return;

		modEventBus.addListener(this::onRegisterClientReloadListeners);
		NeoForge.EVENT_BUS.addListener(this::onClientTick);
	}

	private void onRegisterClientReloadListeners(
		RegisterClientReloadListenersEvent event)
	{
		initWurst();
	}

	private void onClientTick(ClientTickEvent.Post event)
	{
		initWurst();
	}

	private static synchronized void initWurst()
	{
		if(initialized)
			return;

		initialized = true;
		WurstClient.INSTANCE.initialize();
	}
}