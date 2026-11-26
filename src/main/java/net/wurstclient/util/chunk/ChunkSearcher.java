/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util.chunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.dimension.DimensionType;
import net.wurstclient.util.MinPriorityThreadFactory;

/**
 * Searches the given {@link ChunkAccess} for blocks matching the given query.
 */
public final class ChunkSearcher
{
	private static final ExecutorService BACKGROUND_THREAD_POOL =
		MinPriorityThreadFactory.newFixedThreadPool();
	
	private final BiPredicate<BlockPos, BlockState> query;
	private final ChunkAccess chunk;
	private final DimensionType dimension;
	
	private CompletableFuture<ArrayList<Result>> future;
	private boolean interrupted;
	
	public ChunkSearcher(BiPredicate<BlockPos, BlockState> query,
		ChunkAccess chunk, DimensionType dimension)
	{
		this.query = query;
		this.chunk = chunk;
		this.dimension = dimension;
	}
	
	public void start()
	{
		if(future != null || interrupted)
			throw new IllegalStateException();
		
		future = CompletableFuture.supplyAsync(this::searchNow,
			BACKGROUND_THREAD_POOL);
	}
	
	private ArrayList<Result> searchNow()
	{
		ArrayList<Result> results = new ArrayList<>();
		
		// The chunk can be unloaded or rebuilt by the main thread while we are
		// scanning it, which makes getBlockState() throw
		// MissingPaletteEntryException. Returning the partial results instead
		// of crashing is fine: the chunk will be searched again once it
		// updates.
		try
		{
			ChunkPos chunkPos = chunk.getPos();
			
			int minX = chunkPos.getMinBlockX();
			int minY = chunk.getMinBuildHeight();
			int minZ = chunkPos.getMinBlockZ();
			int maxX = chunkPos.getMaxBlockX();
			int maxY = ChunkUtils.getHighestNonEmptySectionYOffset(chunk) + 16;
			int maxZ = chunkPos.getMaxBlockZ();
			
			for(int x = minX; x <= maxX; x++)
				for(int y = minY; y <= maxY; y++)
					for(int z = minZ; z <= maxZ; z++)
					{
						if(interrupted)
							return results;
						
						BlockPos pos = new BlockPos(x, y, z);
						BlockState state = chunk.getBlockState(pos);
						if(!query.test(pos, state))
							continue;
						
						results.add(new Result(pos, state));
					}
			
		}catch(RuntimeException e)
		{
			// Chunk got unloaded/changed mid-scan - keep what we have.
		}
		
		return results;
	}
	
	public void cancel()
	{
		if(future == null || future.isDone())
			return;
		
		interrupted = true;
		future.cancel(false);
	}
	
	public boolean isInterrupted()
	{
		return interrupted;
	}
	
	public ChunkPos getPos()
	{
		return chunk.getPos();
	}
	
	public DimensionType getDimension()
	{
		return dimension;
	}
	
	public Stream<Result> getMatches()
	{
		if(future == null || future.isCancelled())
			return Stream.empty();
		
		try
		{
			return future.join().stream();
			
		}catch(RuntimeException e)
		{
			return Stream.empty();
		}
	}
	
	public List<Result> getMatchesList()
	{
		if(future == null || future.isCancelled())
			return List.of();
		
		try
		{
			return Collections.unmodifiableList(future.join());
			
		}catch(RuntimeException e)
		{
			return List.of();
		}
	}
	
	public boolean isDone()
	{
		return future != null && future.isDone();
	}
	
	public record Result(BlockPos pos, BlockState state)
	{}
}
