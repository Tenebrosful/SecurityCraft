package net.geforcemods.securitycraft.blocks.reinforced;

import java.util.Collections;
import java.util.List;

import net.geforcemods.securitycraft.blockentities.ReinforcedPistonBlockEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.MovingPistonBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public class ReinforcedMovingPistonBlock extends MovingPistonBlock {
	public ReinforcedMovingPistonBlock(AbstractBlock.Properties properties) {
		super(properties);
	}

	public static TileEntity createTilePiston(BlockState state, CompoundNBT tag, Direction direction, boolean extending, boolean shouldHeadBeRendered) {
		return new ReinforcedPistonBlockEntity(state, tag, direction, extending, shouldHeadBeRendered);
	}

	@Override
	public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock()) {
			TileEntity te = world.getBlockEntity(pos);

			if (te instanceof ReinforcedPistonBlockEntity)
				((ReinforcedPistonBlockEntity) te).clearPistonTileEntity();
		}
	}

	@Override
	public void destroy(IWorld world, BlockPos pos, BlockState state) {
		BlockPos oppositePos = pos.relative(state.getValue(FACING).getOpposite());
		BlockState oppositeState = world.getBlockState(oppositePos);

		if (oppositeState.getBlock() instanceof ReinforcedPistonBlock && oppositeState.getValue(PistonBlock.EXTENDED))
			world.removeBlock(oppositePos, false);
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
		ReinforcedPistonBlockEntity reinforcedPistonTileEntity = this.getTileEntity(builder.getLevel(), new BlockPos(builder.getParameter(LootParameters.ORIGIN)));
		return reinforcedPistonTileEntity == null ? Collections.emptyList() : reinforcedPistonTileEntity.getPistonState().getDrops(builder);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		ReinforcedPistonBlockEntity reinforcedPistonTileEntity = this.getTileEntity(worldIn, pos);
		return reinforcedPistonTileEntity != null ? reinforcedPistonTileEntity.getCollisionShape(worldIn, pos) : VoxelShapes.empty();
	}

	private ReinforcedPistonBlockEntity getTileEntity(IBlockReader world, BlockPos pos) {
		TileEntity tileentity = world.getBlockEntity(pos);
		return tileentity instanceof ReinforcedPistonBlockEntity ? (ReinforcedPistonBlockEntity) tileentity : null;
	}
}
