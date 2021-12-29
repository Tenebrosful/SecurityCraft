package net.geforcemods.securitycraft.blocks;

import java.util.Random;

import net.geforcemods.securitycraft.tileentity.KeypadBlastFurnaceTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class KeypadBlastFurnaceBlock extends AbstractKeypadFurnaceBlock {
	public KeypadBlastFurnaceBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void animateTick(BlockState state, World world, BlockPos pos, Random rand) {
		if (state.get(LIT) && getExtendedState(state, world, pos).getBlock() == this) {
			double x = pos.getX() + 0.5D;
			double y = pos.getY();
			double z = pos.getZ() + 0.5D;

			if (rand.nextDouble() < 0.1D)
				world.playSound(x, y, z, SoundEvents.BLOCK_BLASTFURNACE_FIRE_CRACKLE, SoundCategory.BLOCKS, 1.0F, 1.0F, false);

			if (state.get(OPEN)) {
				Direction direction = state.get(FACING);
				Direction.Axis axis = direction.getAxis();
				double randomNumber = rand.nextDouble() * 0.6D - 0.3D;
				double xOffset = axis == Direction.Axis.X ? direction.getXOffset() * 0.32D : randomNumber;
				double yOffset = rand.nextDouble() * 9.0D / 16.0D;
				double zOffset = axis == Direction.Axis.Z ? direction.getZOffset() * 0.32D : randomNumber;
				world.addParticle(ParticleTypes.SMOKE, x + xOffset, y + yOffset, z + zOffset, 0.0D, 0.0D, 0.0D);
			}
		}
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new KeypadBlastFurnaceTileEntity();
	}
}