package net.geforcemods.securitycraft.blocks.reinforced;

import java.util.function.Supplier;

import net.geforcemods.securitycraft.blockentities.BlockPocketBlockEntity;
import net.geforcemods.securitycraft.util.IBlockPocket;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;

public class ReinforcedRotatedCrystalQuartzPillar extends ReinforcedRotatedPillarBlock implements IBlockPocket {
	public ReinforcedRotatedCrystalQuartzPillar(AbstractBlock.Properties properties, Supplier<Block> vB) {
		super(properties, vB);
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new BlockPocketBlockEntity();
	}
}
