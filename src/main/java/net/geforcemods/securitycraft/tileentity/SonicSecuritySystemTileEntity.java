package net.geforcemods.securitycraft.tileentity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.api.CustomizableTileEntity;
import net.geforcemods.securitycraft.api.ILockable;
import net.geforcemods.securitycraft.api.Option;
import net.geforcemods.securitycraft.api.Option.BooleanOption;
import net.geforcemods.securitycraft.misc.ModuleType;
import net.geforcemods.securitycraft.misc.SCSounds;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

public class SonicSecuritySystemTileEntity extends CustomizableTileEntity {

	// The delay between each ping sound in ticks
	private static final int DELAY = 100;

	// How far away can this SSS reach (possibly a config option?)
	public static final int MAX_RANGE = 30;

	// How many blocks can be linked to a SSS (another config option?)
	public static final int MAX_LINKED_BLOCKS = 30;

	// Whether the ping sound should be emitted or not
	private BooleanOption isSilent = new BooleanOption("isSilent", false);

	private int cooldown = DELAY;
	public float radarRotationDegrees = 0;

	// A list containing all of the blocks that this SSS is linked to
	public Set<BlockPos> linkedBlocks = new HashSet<>();

	// Is this SSS active? Not used yet but will be in the future to allow
	// the player to disable the SSS
	private boolean isActive = true;

	public SonicSecuritySystemTileEntity()
	{
		super(SCContent.teTypeSonicSecuritySystem);
	}

	@Override
	public void tick()
	{
		if(!world.isRemote)
		{
			// If this SSS isn't linked to any blocks, return as no sound should
			// be emitted and no blocks need to be removed
			if(!isLinkedToBlock())
				return;

			if(cooldown > 0)
			{
				cooldown--;
			}
			else
			{
				// TODO: should the SSS automatically forget the positions of linked blocks
				// if they are broken?
				ArrayList<BlockPos> blocksToRemove = new ArrayList<BlockPos>();
				Iterator<BlockPos> iterator = linkedBlocks.iterator();

				while(iterator.hasNext())
				{
					BlockPos blockPos = iterator.next();

					if(!(world.getTileEntity(blockPos) instanceof ILockable))
						blocksToRemove.add(blockPos);
				}

				// This delinking part is in a separate loop to prevent a ConcurrentModificationException
				for(BlockPos posToRemove : blocksToRemove)
				{
					delink(posToRemove, false);
					sync();
				}

				// Play the ping sound if it was not disabled
				if(!isSilent.get())
					world.playSound(null, pos, SCSounds.PING.event, SoundCategory.BLOCKS, 0.3F, 1.0F);

				cooldown = DELAY;
			}
		}
		else
		{
			// Turn the radar dish slightly
			radarRotationDegrees += 0.15;

			if(radarRotationDegrees >= 360)
				radarRotationDegrees = 0;
		}
	}

	@Override
	public CompoundNBT write(CompoundNBT tag)
	{
		super.write(tag);

		// If there are blocks to save but the tag doesn't have a CompoundNBT
		// to store them in, create one (shouldn't be needed)
		if(linkedBlocks.size() > 0 && !tag.contains("LinkedBlocks"))
		{
			tag.put("LinkedBlocks", new ListNBT());
		}

		Iterator<BlockPos> iterator = linkedBlocks.iterator();

		while(iterator.hasNext())
		{
			BlockPos blockToSave = iterator.next();

			CompoundNBT nbt = NBTUtil.writeBlockPos(blockToSave);

			tag.getList("LinkedBlocks", Constants.NBT.TAG_COMPOUND).add(nbt);

			if(!linkedBlocks.contains(blockToSave))
				linkedBlocks.add(blockToSave);
		}

		tag.putBoolean("isActive", isActive);

		return tag;
	}

	@Override
	public void read(BlockState state, CompoundNBT tag)
	{
		super.read(state, tag);

		if(tag.contains("LinkedBlocks"))
		{
			ListNBT list = tag.getList("LinkedBlocks", Constants.NBT.TAG_COMPOUND);

			// Read each saved position and add it to the linkedBlocks list
			for(int i = 0; i < list.size(); i++)
			{
				CompoundNBT linkedBlock = list.getCompound(i);

				BlockPos linkedBlockPos = NBTUtil.readBlockPos(linkedBlock);

				linkedBlocks.add(linkedBlockPos);
			}
		}

		isActive = tag.getBoolean("isActive");
	}

	/**
	 * Copies the positions over from the SSS item's tag into this TileEntity.
	 */
	public void transferPositionsFromItem(CompoundNBT itemTag)
	{
		if(itemTag == null || !itemTag.contains("LinkedBlocks"))
			return;

		ListNBT blocks = itemTag.getList("LinkedBlocks", Constants.NBT.TAG_COMPOUND);

		for(int i = 0; i < blocks.size(); i++)
		{
			CompoundNBT linkedBlock = blocks.getCompound(i);
			BlockPos linkedBlockPos = NBTUtil.readBlockPos(linkedBlock);

			// If the block has not already been linked with, add it to the list
			if(!isLinkedToBlock(linkedBlockPos))
			{
				linkedBlocks.add(linkedBlockPos);
			}
		}

		sync();
	}

	/**
	 * @return If this Sonic Security System is linked to another block
	 */
	public boolean isLinkedToBlock()
	{
		return !linkedBlocks.isEmpty();
	}

	/**
	 * @return If this Sonic Security System is linked to a block at a specific position
	 */
	public boolean isLinkedToBlock(BlockPos linkedBlockPos)
	{
		if(linkedBlocks.isEmpty())
			return false;

		return linkedBlocks.contains(linkedBlockPos);
	}

	/**
	 * Delinks this Sonic Security System from the given block
	 */
	public void delink(BlockPos linkedBlockPos, boolean shouldSync)
	{
		if(linkedBlocks.isEmpty())
			return;

		linkedBlocks.remove(linkedBlockPos);

		if(shouldSync)
			sync();
	}

	/**
	 * Delinks this Sonic Security System from all other blocks
	 */
	public void delinkAll()
	{
		linkedBlocks.clear();
		sync();
	}

	public int getNumberOfLinkedBlocks()
	{
		return linkedBlocks.size();
	}

	/**
	 * Toggles this Sonic Security System on or off (not used yet)
	 */
	public void toggle()
	{
		isActive = !isActive;
		sync();
	}

	/**
	 * @return Is this Sonic Security System active?
	 */
	public boolean isActive()
	{
		return isActive;
	}

	@Override
	public ModuleType[] acceptedModules()
	{
		return new ModuleType[]{};
	}

	@Override
	public Option<?>[] customOptions()
	{
		return new Option[] { isSilent };
	}

}
